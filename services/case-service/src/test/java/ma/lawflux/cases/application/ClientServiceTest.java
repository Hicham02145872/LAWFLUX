package ma.lawflux.cases.application;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ma.lawflux.cases.domain.Client;
import ma.lawflux.cases.domain.ClientRepository;
import ma.lawflux.cases.domain.ClientStatus;

class ClientServiceTest {
    private final ClientRepository repository = mock(ClientRepository.class);
    private final ClientService service = new ClientService(repository);

    private final UUID userId = UUID.randomUUID();
    private final UUID cabinetId = UUID.randomUUID();

    private CaseCaller manager() {
        return new CaseCaller(userId, cabinetId, Set.of("cabinet_manager"));
    }

    @Test
    void createTrimsFieldsAndUsesCallersCabinet() {
        UUID clientId = UUID.randomUUID();
        Instant now = Instant.now();

        Client saved = new Client(
                clientId, cabinetId, "Fatima", "fatima@example.com",
                "0600000000", "Casa", ClientStatus.ACTIVE, now, now, null
        );

        when(repository.create(
                cabinetId, "Fatima", "fatima@example.com", "0600000000", "Casa"
        )).thenReturn(saved);

        Client result = service.create(
                manager(), " Fatima ", " fatima@example.com ",
                " 0600000000 ", " Casa "
        );

        assertEquals(clientId, result.id());
        assertEquals("Fatima", result.name());
        verify(repository).create(
                cabinetId, "Fatima", "fatima@example.com", "0600000000", "Casa"
        );
    }

    @Test
    void rejectsCallerWhoIsNotCabinetManager() {
        CaseCaller lawyer = new CaseCaller(userId, cabinetId, Set.of("lawyer"));

        assertThrows(SecurityException.class,
                () -> service.create(lawyer, "Fatima", null, null, null));

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsBlankClientName() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(manager(), "   ", null, null, null));

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsInvalidPagination() {
        assertThrows(IllegalArgumentException.class,
                () -> service.list(manager(), null, 0, 20));

        assertThrows(IllegalArgumentException.class,
                () -> service.list(manager(), null, 1, 101));

        verifyNoInteractions(repository);
    }

    @Test
    void listUsesCabinetAndReturnsPaginationData() {
        when(repository.findPage(cabinetId, ClientStatus.ACTIVE, 10, 10))
                .thenReturn(List.of());
        when(repository.count(cabinetId, ClientStatus.ACTIVE)).thenReturn(21L);

        ClientPage result = service.list(manager(), ClientStatus.ACTIVE, 2, 10);

        assertEquals(2, result.page());
        assertEquals(10, result.pageSize());
        assertEquals(21, result.totalElements());
        assertEquals(3, result.totalPages());
        verify(repository).findPage(cabinetId, ClientStatus.ACTIVE, 10, 10);
    }
}