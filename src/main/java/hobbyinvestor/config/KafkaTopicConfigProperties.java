package hobbyinvestor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hobbyinvestor.topic-names")
@Data
public class KafkaTopicConfigProperties {
    private String companyProfileUpdateTopic;
    private String companyFinancialsUpdateTopic;
    private String companyNewsUpdateTopic;
}
