package hobbyinvestor.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "finnhub-client")
public interface FinnhubStocksClient {
    @GetMapping("/symbol")
        String getStockSymbols(@RequestParam(value = "exchange") String exchange,
                                  @RequestParam(value = "token") String apikey);
}