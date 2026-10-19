package at.technikum.dms.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String DOCUMENT_CREATED_QUEUE = "document.created";

    @Bean
    public Queue documentCreatedQueue() {
        return new Queue(DOCUMENT_CREATED_QUEUE, true);
    }

    /** Nachrichten als JSON statt Java-Serialisierung/Text; wird von Spring Boot automatisch im RabbitTemplate verwendet. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
