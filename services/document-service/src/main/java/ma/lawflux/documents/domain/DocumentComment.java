package ma.lawflux.documents.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentComment(
        UUID id,
        UUID documentId,
        UUID cabinetId,
        UUID authorId,
        String authorName,
        CommentVisibility visibility,
        String body,
        Instant createdAt) {
}