package ma.lawflux.platform.events;
import java.time.Instant;
import java.util.UUID;
/** Message technique sans contenu client ; ne remplace pas les événements métier multi-tenant. */
public record ScaffoldEvent(UUID eventId, Instant occurredAt, String correlationId, String type) {}
