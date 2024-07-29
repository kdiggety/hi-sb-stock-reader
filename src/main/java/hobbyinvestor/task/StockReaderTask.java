package hobbyinvestor.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import hobbyinvestor.client.FinnhubStocksClient;
import hobbyinvestor.config.KafkaTopicConfigProperties;
import hobbyinvestor.model.StockSymbol;
import hobbyinvestor.repository.StockSymbolPagingRepository;
import hobbyinvestor.repository.StockSymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
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
    public static final String HOBBYINVESTOR_TASK_STOCK_SYMBOLS_PAGE_NUMBER = "hobbyinvestor.task.stock-symbols.page-number";
    private final FinnhubStocksClient finnhubStocksClient;
    private final StockSymbolRepository stockSymbolRepository;
    private final StockSymbolPagingRepository stockSymbolPagingRepository;
    private final KafkaTemplate kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicConfigProperties kafkaTopicConfigProperties;
    @Qualifier("redisTemplateNumbers") private final RedisTemplate redisTemplate;

    //@Scheduled(cron = "${hobbyinvestor.tasks.stock-symbols.cron}")
    public void processStockSymbols() throws IOException {
        log.info("Starting stock symbols reader task...");
        String response = finnhubStocksClient.getStockSymbols("US", FINNHUB_ACCESS_KEY);
        StockSymbol[] symbols = objectMapper.readValue(response, StockSymbol[].class);
        log.info("Saving {} symbols to Redis...", symbols.length);
        stockSymbolRepository.saveAll(List.of(symbols));
        log.info("Done.");
    }

    @Scheduled(cron = "${hobbyinvestor.tasks.company-details.cron}")
    public void batchProcessCompanyDetails() {
        log.info("Starting company details processing task...");

        // Figure out the current page number
        int pageNumber = 1;
        if (redisTemplate.hasKey(HOBBYINVESTOR_TASK_STOCK_SYMBOLS_PAGE_NUMBER)) {
            Object pageNumberString = redisTemplate.opsForValue().get(HOBBYINVESTOR_TASK_STOCK_SYMBOLS_PAGE_NUMBER);
            log.info("Current page number string is {}", pageNumberString);
            pageNumber = Integer.valueOf(String.valueOf(pageNumberString));
            log.info("Current page number is {}", pageNumber);
        } else {
            log.info("Creating page number key with value {}", pageNumber);
            redisTemplate.opsForValue().set(HOBBYINVESTOR_TASK_STOCK_SYMBOLS_PAGE_NUMBER, String.valueOf(pageNumber));
        }

        // Get the stock symbols for the current page
        Pageable pageable = PageRequest.of(pageNumber, 10, Sort.by("symbol"));
        Page<StockSymbol> page = stockSymbolPagingRepository.findAll(pageable);
        log.info("Processing {} stock symbols", page.getSize());
        log.info("Total page count is {}", page.getTotalPages());
        page.stream().forEach(stockSymbol -> {
            log.info("Processing company stock symbol {}", stockSymbol);
            CompletableFuture.allOf(
                    CompletableFuture.supplyAsync(() -> finnhubStocksClient.getCompanyProfile(stockSymbol.getSymbol(), FINNHUB_ACCESS_KEY))
                            .thenAccept(message -> sendMessage(kafkaTopicConfigProperties.getCompanyProfileUpdateTopic(), message))
                            .exceptionallyAsync(throwable -> {
                                log.error("Failed to get company profile!", throwable);
                                return null;
                            }),
                    CompletableFuture.supplyAsync(() -> finnhubStocksClient.getStockSymbolFinancials(stockSymbol.getSymbol(), ALL_METRICS, FINNHUB_ACCESS_KEY))
                            .thenAccept(message -> sendMessage(kafkaTopicConfigProperties.getCompanyFinancialsUpdateTopic(), message))
                            .exceptionallyAsync(throwable -> {
                                log.error("Failed to get company financials!", throwable);
                                return null;
                            }),
                    CompletableFuture.supplyAsync(() -> finnhubStocksClient.getCompanyNews(stockSymbol.getSymbol(), "2024-07-01", "2024-07-02", FINNHUB_ACCESS_KEY))
                            .thenAccept(message -> sendMessage(kafkaTopicConfigProperties.getCompanyNewsUpdateTopic(), message))
                            .exceptionallyAsync(throwable -> {
                                log.error("Failed to get company news!", throwable);
                                return null;
                            })
            ).join();
        });

        // Increment the page number
        int nextPageNumber = pageNumber + 1;
        log.info("Incrementing Page number to {}", nextPageNumber);
        redisTemplate.opsForValue().set(HOBBYINVESTOR_TASK_STOCK_SYMBOLS_PAGE_NUMBER, String.valueOf(nextPageNumber));
        log.info("Done.");
    }

    @Async
    public void sendMessage(String topicName, String message) {
        if (StringUtils.isNotEmpty(message) && !"[]".equals(message)) {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topicName, message);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent Message: {}; Topic: {}; Offset: {}", message, topicName, result.getRecordMetadata().offset());
                } else {
                    log.error("Unable to Send Message: {}; Topic: {}", message, topicName, ex);
                }
            });
        } else {
            log.warn("Not sending empty message to Topic: {}!", topicName);
        }
    }
}