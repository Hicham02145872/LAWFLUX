package ma.lawflux.notifications.application;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.lawflux.platform.events.ScaffoldEvent;
import ma.lawflux.notifications.infrastructure.ScaffoldRabbitTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
public class ScaffoldEventListener {
    private static final Logger log = LoggerFactory.getLogger(ScaffoldEventListener.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public ScaffoldEventListener(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }
    @RabbitListener(queues = ScaffoldRabbitTopology.QUEUE)
    public void consume(String json) {
        ScaffoldEvent event;
        try {
            event = mapper.readValue(json, ScaffoldEvent.class);
            if (event.eventId() == null || event.occurredAt() == null || !"scaffold.ping".equals(event.type())) throw new IllegalArgumentException();
        } catch (Exception ex) { throw new AmqpRejectAndDontRequeueException("Événement technique invalide"); }
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", ma.lawflux.platform.web.CorrelationFilter.normalize(event.correlationId()))) {
            // Autocommit avant retour : une redélivrance éventuelle reste sans doublon.
            int inserted = jdbc.update("INSERT INTO scaffold_event_receipt(event_id, occurred_at) VALUES (?, ?) ON CONFLICT (event_id) DO NOTHING",
                event.eventId(), java.sql.Timestamp.from(event.occurredAt()));
            log.info("scaffold_event_received eventId={} inserted={}", event.eventId(), inserted);
        }
    }
}
