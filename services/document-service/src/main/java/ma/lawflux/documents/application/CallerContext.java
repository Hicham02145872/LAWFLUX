package ma.lawflux.documents.application;

import java.util.UUID;

/** Identité déduite du jeton Bearer (metadata gRPC), jamais lue depuis le message client. */
public record CallerContext(UUID userId, UUID cabinetId, String authorName) {
}