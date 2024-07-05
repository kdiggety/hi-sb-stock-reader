package hobbyinvestor.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hobbyinvestor.client.FinnhubStocksClient;
import hobbyinvestor.model.StockSymbol;
import hobbyinvestor.model.StockSymbolSearchResults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReaderTask {
    private final FinnhubStocksClient finnhubStocksClient;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "${hobbyinvestor.tasks.stock-reader.cron}")
    public void process() throws IOException {
        log.info("Starting stock reader task...");
        String response = finnhubStocksClient.getStockSymbols("US", "cq3gnshr01qobiism7p0cq3gnshr01qobiism7pg");
        StockSymbol[] symbols = objectMapper.readValue(response, StockSymbol[].class);
        log.info("" + symbols.length);
    }
}