
package hobbyinvestor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(value = { "currency", "figi", "isin", "mic", "shareClassFIGI", "symbol2" })
public class StockSymbol {
    private String description;
    private String displaySymbol;
    private String symbol;
    private String type;
}
