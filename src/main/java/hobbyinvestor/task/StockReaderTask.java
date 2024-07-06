package hobbyinvestor.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hobbyinvestor.client.FinnhubStocksClient;
import hobbyinvestor.model.StockSymbol;
import hobbyinvestor.model.StockSymbolFinancials;
import hobbyinvestor.repository.StockSymbolFinancialsRepository;
import hobbyinvestor.repository.StockSymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

import static hobbyinvestor.constant.FinnhubStocksConstants.ALL_METRICS;
import static hobbyinvestor.constant.FinnhubStocksConstants.ARBITRARY_SLEEP_MILLIS;
import static hobbyinvestor.constant.FinnhubStocksConstants.FINNHUB_ACCESS_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReaderTask {
    private final FinnhubStocksClient finnhubStocksClient;
    private final StockSymbolRepository stockSymbolRepository;
    private final ObjectMapper objectMapper;
    private final StockSymbolFinancialsRepository stockSymbolFinancialsRepository;

    @Scheduled(cron = "${hobbyinvestor.tasks.stock-symbols.cron}")
    public void processStockSymbols() throws IOException {
        log.info("Starting stock symbols reader task...");
        String response = finnhubStocksClient.getStockSymbols("US", FINNHUB_ACCESS_KEY);
        StockSymbol[] symbols = objectMapper.readValue(response, StockSymbol[].class);
        log.info(String.format("Saving %d symbols to Redis...", symbols.length));
        stockSymbolRepository.saveAll(List.of(symbols));
        log.info("Done.");
    }

    @Scheduled(cron = "${hobbyinvestor.tasks.stock-symbols-financials.cron}")
    public void processStockSymbolsFinancials() throws IOException {
        log.info("Starting stock symbol financials reader task...");
        Iterable<StockSymbol> stockSymbols = stockSymbolRepository.findAll();
        if (stockSymbols != null) {
            StreamSupport.stream(stockSymbols.spliterator(), false).forEach(stockSymbol -> {
                String response =
                        finnhubStocksClient.getStockSymbolFinancials(stockSymbol.getSymbol(),
                                ALL_METRICS, FINNHUB_ACCESS_KEY);

                StockSymbolFinancials stockSymbolFinancials;
                try {
                    JsonNode jsonNode = objectMapper.readTree(response);

                    stockSymbolFinancials =
                            objectMapper.treeToValue(jsonNode.get("metric"), StockSymbolFinancials.class);
                    stockSymbolFinancials.setSymbol(stockSymbol.getSymbol());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                log.info(String.format("Saving '%s' (%s) financials to Redis...",
                        stockSymbol.getDescription(), stockSymbol.getSymbol()));
                stockSymbolFinancialsRepository.save(stockSymbolFinancials);

                try {
                    Thread.sleep(ARBITRARY_SLEEP_MILLIS);
                } catch (InterruptedException e) {
                    log.error("Rude awakening! ...", e);
                }
            });
        }
        log.info("Done.");
    }
}