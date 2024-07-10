package hobbyinvestor.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {
    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    private final KafkaTopicConfigProperties kafkaTopicConfigProperties;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic companyProfileUpdateTopic() {
        return new NewTopic(kafkaTopicConfigProperties.getCompanyProfileUpdateTopic(), 1, (short) 1);
    }

    @Bean
    public NewTopic companyFinancialsUpdateTopic() {
        return new NewTopic(kafkaTopicConfigProperties.getCompanyFinancialsUpdateTopic(), 1, (short) 1);
    }

    @Bean
    public NewTopic companyNewsUpdateTopic() {
        return new NewTopic(kafkaTopicConfigProperties.getCompanyNewsUpdateTopic(), 1, (short) 1);
    }
}
