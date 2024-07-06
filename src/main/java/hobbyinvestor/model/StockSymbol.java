
package hobbyinvestor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Data
@RedisHash("StockSymbol")
@JsonIgnoreProperties(value = { "currency", "figi", "isin", "mic", "shareClassFIGI", "symbol2" })
public class StockSymbol {
    @Id
    private String symbol;

    private String description;
    private String displaySymbol;
    private String type;
}