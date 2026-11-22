package ma.lawflux.cases.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {

    Optional<Client> findById(UUID cabinetId, UUID clientId);

    List<Client> findPage(
            UUID cabinetId,
            ClientStatus status,
            int limit,
            int offset
    );

    long count(UUID cabinetId, ClientStatus status);

    Client create(
            UUID cabinetId,
            String name,
            String email,
            String phone,
            String address
    );

    boolean update(
            UUID cabinetId,
            UUID clientId,
            String name,
            String email,
            String phone,
            String address
    );

    boolean archive(UUID cabinetId, UUID clientId);
}