package hobbyinvestor.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import hobbyinvestor.client.FinnhubStocksClient;
import hobbyinvestor.config.KafkaTopicConfigProperties;
import hobbyinvestor.model.StockSymbol;
import hobbyinvestor.repository.StockSymbolPagingRepository;
import hobbyinvestor.repository.StockSymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static hobbyinvestor.constant.FinnhubStocksConstants.ALL_METRICS;
import static hobbyinvestor.constant.FinnhubStocksConstants.FINNHUB_ACCESS_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReaderTask {
    private final FinnhubStocksClient finnhubStocksClient;
    private final StockSymbolRepository stockSymbolRepository;
    private final StockSymbolPagingRepository stockSymbolPagingRepository;
    private final KafkaTemplate kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicConfigProperties kafkaTopicConfigProperties;

    //@Scheduled(cron = "${hobbyinvestor.tasks.stock-symbols.cron}")
    public void processStockSymbols() throws IOException {
        log.info("Starting stock symbols reader task...");
        String response = finnhubStocksClient.getStockSymbols("US", FINNHUB_ACCESS_KEY);
        StockSymbol[] symbols = objectMapper.readValue(response, StockSymbol[].class);
        log.info(String.format("Saving %d symbols to Redis...", symbols.length));
        stockSymbolRepository.saveAll(List.of(symbols));
        log.info("Done.");
    }

    @Scheduled(cron = "${hobbyinvestor.tasks.company-details.cron}")
    public void batchProcessCompanyDetails() {
        log.info("Starting company details processing task...");
        Pageable pageable = PageRequest.of(1, 10);
        Page<StockSymbol> page = stockSymbolPagingRepository.findAll(pageable);
        log.info(String.format("Processing %d pages...", page.getTotalPages()));
        page.stream().forEach(stockSymbol -> {
            log.info(String.format("Processing company stock symbol %s", stockSymbol));
            CompletableFuture.allOf(
                    CompletableFuture.supplyAsync(() -> finnhubStocksClient.getCompanyProfile(stockSymbol.getSymbol(), FINNHUB_ACCESS_KEY))
                            .thenAccept(message -> sendMessage(kafkaTopicConfigProperties.getCompanyProfileUpdateTopic(), message)),
                    CompletableFuture.supplyAsync(() -> finnhubStocksClient.getStockSymbolFinancials(stockSymbol.getSymbol(), ALL_METRICS, FINNHUB_ACCESS_KEY))
                            .thenAccept(message -> sendMessage(kafkaTopicConfigProperties.getCompanyFinancialsUpdateTopic(), message)),
                    CompletableFuture.supplyAsync(() -> finnhubStocksClient.getCompanyNews(stockSymbol.getSymbol(), "2024-07-01", "2024-07-02", FINNHUB_ACCESS_KEY))
                            .thenAccept(message -> sendMessage(kafkaTopicConfigProperties.getCompanyNewsUpdateTopic(), message))
            ).join();
        });
        log.info("Done.");
    }

    @Async
    public void sendMessage(String topicName, String message) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topicName, message);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info(String.format("Sent Message: %s; Offset: %d", message, result.getRecordMetadata().offset()));
            } else {
                log.error(String.format("Unable to Send Message: %s", message), ex);
            }
        });
    }
}