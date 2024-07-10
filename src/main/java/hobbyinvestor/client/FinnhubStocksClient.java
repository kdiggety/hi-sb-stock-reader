package hobbyinvestor.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "finnhub-client")
public interface FinnhubStocksClient {
    @GetMapping("/stock/symbol")
        String getStockSymbols(@RequestParam(value = "exchange") String exchange,
                                  @RequestParam(value = "token") String apikey);

    @GetMapping("/stock/profile2?symbol={symbol}")
    String getCompanyProfile(@PathVariable() String symbol, @RequestParam(value = "token") String apikey);

    @GetMapping("/stock/metric?symbol={symbol}")
    String getStockSymbolFinancials(@PathVariable() String symbol,
                                    @RequestParam(value = "metric") String metric,
                                    @RequestParam(value = "token") String apikey);

    @GetMapping("/company-news?symbol={symbol}&from={from}&to={to}")
    String getCompanyNews(@PathVariable() String symbol,
                          @PathVariable() String from,
                          @PathVariable() String to,
                          @RequestParam(value = "token") String apikey);
}