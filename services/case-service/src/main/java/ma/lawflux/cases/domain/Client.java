package ma.lawflux.cases.domain;

import java.time.Instant;
import java.util.UUID;

public record Client(
        UUID id,
        UUID cabinetId,
        String name,
        String email,
        String phone,
        String address,
        ClientStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
) {}