package br.com.itau.compraprogramada.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.ir:ir-eventos}")
    private String topicIr;

    @Bean
    public NewTopic topicIrEventos() {
        return TopicBuilder.name(topicIr)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
