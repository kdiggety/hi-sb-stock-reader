package hobbyinvestor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class StockSymbolSearchResults {
    private int count;

    @JsonProperty("result")
    private List<StockSymbol> stockSymbols;
}
