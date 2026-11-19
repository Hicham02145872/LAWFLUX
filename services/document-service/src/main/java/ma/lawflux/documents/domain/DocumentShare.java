package ma.lawflux.documents.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentShare(
        UUID id,
        UUID documentId,
        UUID cabinetId,
        UUID sharedBy,
        Instant sharedAt) {
}