package ma.lawflux.cases.application;

import java.util.NoSuchElementException;
import java.util.UUID;
import ma.lawflux.cases.domain.Client;
import ma.lawflux.cases.domain.ClientRepository;
import ma.lawflux.cases.domain.ClientStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Client create(
            CaseCaller caller,
            String name,
            String email,
            String phone,
            String address
    ) {
        requireManager(caller);
        ClientInput input = normalize(name, email, phone, address);

        return repository.create(
                caller.cabinetId(),
                input.name(),
                input.email(),
                input.phone(),
                input.address()
        );
    }

    @Transactional(readOnly = true)
    public Client get(CaseCaller caller, UUID clientId) {
        requireManager(caller);
        return repository.findById(caller.cabinetId(), requireId(clientId))
                .orElseThrow(() -> new NoSuchElementException("Client introuvable"));
    }

    @Transactional(readOnly = true)
    public ClientPage list(
            CaseCaller caller,
            ClientStatus status,
            int page,
            int pageSize
    ) {
        requireManager(caller);

        if (page < 1) throw new IllegalArgumentException("La page doit commencer à 1");
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("La taille de page doit être entre 1 et 100");
        }

        long offsetLong = (long) (page - 1) * pageSize;
        if (offsetLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Numéro de page trop grand");
        }

        int offset = (int) offsetLong;
        return new ClientPage(
                repository.findPage(caller.cabinetId(), status, pageSize, offset),
                page,
                pageSize,
                repository.count(caller.cabinetId(), status)
        );
    }

    @Transactional
    public Client update(
            CaseCaller caller,
            UUID clientId,
            String name,
            String email,
            String phone,
            String address
    ) {
        requireManager(caller);
        UUID id = requireId(clientId);
        ClientInput input = normalize(name, email, phone, address);

        boolean updated = repository.update(
                caller.cabinetId(), id,
                input.name(), input.email(), input.phone(), input.address()
        );

        if (!updated) throw explainMissingOrArchived(caller.cabinetId(), id);
        return repository.findById(caller.cabinetId(), id)
                .orElseThrow(() -> new NoSuchElementException("Client introuvable"));
    }

    @Transactional
    public void archive(CaseCaller caller, UUID clientId) {
        requireManager(caller);
        UUID id = requireId(clientId);

        if (!repository.archive(caller.cabinetId(), id)) {
            throw explainMissingOrArchived(caller.cabinetId(), id);
        }
    }

    private RuntimeException explainMissingOrArchived(UUID cabinetId, UUID clientId) {
        return repository.findById(cabinetId, clientId)
                .<RuntimeException>map(client ->
                        new IllegalArgumentException("Client déjà archivé"))
                .orElseGet(() -> new NoSuchElementException("Client introuvable"));
    }

    private static void requireManager(CaseCaller caller) {
        if (caller == null || !caller.hasRole("cabinet_manager")) {
            throw new SecurityException("Accès refusé");
        }
    }

    private static UUID requireId(UUID id) {
        if (id == null) throw new IllegalArgumentException("Identifiant client invalide");
        return id;
    }

    private static ClientInput normalize(
            String name,
            String email,
            String phone,
            String address
    ) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) throw new IllegalArgumentException("Le nom est obligatoire");
        if (cleanName.length() > 200) throw new IllegalArgumentException("Nom trop long");

        String cleanEmail = optional(email, 320, "Email");
        if (cleanEmail != null
                && !cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email invalide");
        }

        return new ClientInput(
                cleanName,
                cleanEmail,
                optional(phone, 40, "Téléphone"),
                optional(address, 500, "Adresse")
        );
    }

    private static String optional(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value.trim();
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(label + " trop long");
        }
        return cleaned;
    }

    private record ClientInput(String name, String email, String phone, String address) {}
}