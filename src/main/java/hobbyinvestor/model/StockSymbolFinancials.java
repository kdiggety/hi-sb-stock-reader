
package hobbyinvestor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.Date;

@Data
@RedisHash("StockSymbolFinancials")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockSymbolFinancials {
    @Id
    private String symbol;

    @JsonProperty(value = "52WeekHigh")
    private float fiftyTwoWeekHigh;

    @JsonProperty(value = "52WeekHighDate")
    @JsonFormat(pattern="yyyy-MM-dd")
    private Date fiftyTwoWeekHighDate;

    @JsonProperty(value = "52WeekLow")
    private float fiftyTwoWeekLow;

    @JsonProperty(value = "52WeekLowDate")
    @JsonFormat(pattern="yyyy-MM-dd")
    private Date fiftyTwoWeekLowDate;
}