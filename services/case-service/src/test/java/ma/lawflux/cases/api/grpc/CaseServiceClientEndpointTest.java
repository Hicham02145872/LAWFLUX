package ma.lawflux.cases.api.grpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import ma.lawflux.cases.application.CaseAccessService;
import ma.lawflux.cases.application.CaseCaller;
import ma.lawflux.cases.application.ClientPage;
import ma.lawflux.cases.application.ClientService;
import ma.lawflux.cases.domain.Client;
import ma.lawflux.cases.domain.ClientStatus;
import ma.lawflux.cases.infrastructure.CaseJwtIdentity;
import ma.lawflux.contracts.cases.v1.ClientReply;
import ma.lawflux.contracts.cases.v1.CreateClientRequest;
import ma.lawflux.contracts.cases.v1.GetClientRequest;
import ma.lawflux.contracts.cases.v1.ListClientsReply;
import ma.lawflux.contracts.cases.v1.ListClientsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseServiceClientEndpointTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID cabinetId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    private final CaseCaller caller =
            new CaseCaller(userId, cabinetId, Set.of("cabinet_manager"));
    private final ClientService clients = mock(ClientService.class);
    private final CaseJwtIdentity identity = mock(CaseJwtIdentity.class);

    private final CaseServiceEndpoint endpoint = new CaseServiceEndpoint(
            mock(CaseAccessService.class),
            clients,
            identity
    );

    private final Instant now = Instant.parse("2026-01-01T12:00:00Z");
    private final Client client = new Client(
            clientId, cabinetId, "Fatima", "fatima@example.com",
            "0600000000", "Casa", ClientStatus.ACTIVE, now, now, null
    );

    @BeforeEach
    void authenticateManager() {
        when(identity.current()).thenReturn(caller);
    }

    @Test
    void createClientMapsRequestAndReply() {
        when(clients.create(caller, "Fatima", "fatima@example.com",
                "0600000000", "Casa")).thenReturn(client);

        Recorder<ClientReply> response = new Recorder<>();
        endpoint.createClient(CreateClientRequest.newBuilder()
                .setName("Fatima")
                .setEmail("fatima@example.com")
                .setPhone("0600000000")
                .setAddress("Casa")
                .build(), response);

        assertNull(response.error.get());
        assertEquals(clientId.toString(), response.value.get().getClient().getId());
        assertEquals("Fatima", response.value.get().getClient().getName());
        verify(clients).create(caller, "Fatima", "fatima@example.com",
                "0600000000", "Casa");
    }

    @Test
    void listClientsUsesDefaultPaginationAndMapsClient() {
        when(clients.list(caller, null, 1, 20))
                .thenReturn(new ClientPage(List.of(client), 1, 20, 1));

        Recorder<ListClientsReply> response = new Recorder<>();
        endpoint.listClients(ListClientsRequest.getDefaultInstance(), response);

        assertNull(response.error.get());
        assertEquals(1, response.value.get().getPage());
        assertEquals(20, response.value.get().getPageSize());
        assertEquals(1, response.value.get().getTotalElements());
        assertEquals(clientId.toString(),
                response.value.get().getClients(0).getId());
    }

    @Test
    void invalidClientIdReturnsInvalidArgument() {
        Recorder<ClientReply> response = new Recorder<>();
        endpoint.getClient(GetClientRequest.newBuilder()
                .setClientId("not-a-uuid")
                .build(), response);

        assertEquals(Status.Code.INVALID_ARGUMENT,
                Status.fromThrowable(response.error.get()).getCode());
        verifyNoInteractions(clients);
    }

    private static class Recorder<T> implements StreamObserver<T> {
        final AtomicReference<T> value = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        @Override
        public void onNext(T result) {
            value.set(result);
        }

        @Override
        public void onError(Throwable throwable) {
            error.set(throwable);
        }

        @Override
        public void onCompleted() {
        }
    }
}
