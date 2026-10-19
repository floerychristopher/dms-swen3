package at.technikum.ocrworker.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    /** Muss mit dem Queue-Namen im REST-Server übereinstimmen. */
    public static final String DOCUMENT_CREATED_QUEUE = "document.created";

    /** Queue auch hier deklarieren, damit die Startreihenfolge von Backend und Worker egal ist. */
    @Bean
    public Queue documentCreatedQueue() {
        return new Queue(DOCUMENT_CREATED_QUEUE, true);
    }

    /** JSON-Nachrichten in Java-Objekte umwandeln (Zieltyp = Parameter der @RabbitListener-Methode). */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
