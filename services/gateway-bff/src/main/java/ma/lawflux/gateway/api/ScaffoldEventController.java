package ma.lawflux.gateway.api;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.lawflux.gateway.infrastructure.ScaffoldRabbitTopology;
import ma.lawflux.platform.events.ScaffoldEvent;
import ma.lawflux.platform.web.CorrelationFilter;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnProperty(name="lawflux.scaffold-events-enabled", havingValue="true")
public class ScaffoldEventController {
    private final RabbitTemplate rabbit;
    private final ObjectMapper mapper;
    public ScaffoldEventController(RabbitTemplate rabbit, ObjectMapper mapper) { this.rabbit = rabbit; this.mapper = mapper; }
    @PostMapping("/api/system/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventAccepted publish() {
        UUID id = UUID.randomUUID();
        var event = new ScaffoldEvent(id, Instant.now(), CorrelationFilter.normalize(MDC.get("correlationId")), "scaffold.ping");
        var correlation = new CorrelationData(id.toString());
        try {
            rabbit.convertAndSend(ScaffoldRabbitTopology.EXCHANGE, ScaffoldRabbitTopology.KEY, mapper.writeValueAsString(event), correlation);
            var confirm = correlation.getFuture().get(3, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) throw new IllegalStateException("Message non routé");
            return new EventAccepted(id, "BROKER_CONFIRMED");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Sonde événementielle indisponible");
        }
    }
    public record EventAccepted(UUID eventId, String state) {}
}
