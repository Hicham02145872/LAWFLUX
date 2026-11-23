package ma.lawflux.gateway.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicReference;
import ma.lawflux.contracts.cases.v1.CaseServiceGrpc;
import ma.lawflux.contracts.cases.v1.ClientRecord;
import ma.lawflux.contracts.cases.v1.ClientReply;
import ma.lawflux.contracts.cases.v1.CreateClientRequest;
import ma.lawflux.contracts.cases.v1.ListClientsReply;
import ma.lawflux.contracts.cases.v1.ListClientsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

class ClientControllerTest {
    @Test
    void createAndListForwardRequestsAndBearerToken() throws Exception {
        var authorization = new AtomicReference<String>();
        var createdRequest = new AtomicReference<CreateClientRequest>();
        var listRequest = new AtomicReference<ListClientsRequest>();

        Metadata.Key<String> authKey =
                Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

        ServerInterceptor interceptor = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next
            ) {
                authorization.set(headers.get(authKey));
                return next.startCall(call, headers);
            }
        };

        Server server = ServerBuilder.forPort(0)
                .intercept(interceptor)
                .addService(new CaseServiceGrpc.CaseServiceImplBase() {
                    @Override
                    public void createClient(
                            CreateClientRequest request,
                            StreamObserver<ClientReply> response
                    ) {
                        createdRequest.set(request);

                        if ("denied".equals(request.getName())) {
                            response.onError(Status.PERMISSION_DENIED
                                    .withDescription("Accès refusé")
                                    .asRuntimeException());
                            return;
                        }

                        ClientRecord client = ClientRecord.newBuilder()
                                .setId("client-123")
                                .setName(request.getName())
                                .setEmail(request.getEmail())
                                .setStatus("ACTIVE")
                                .setCreatedAt("2026-01-01T12:00:00Z")
                                .setUpdatedAt("2026-01-01T12:00:00Z")
                                .build();

                        response.onNext(ClientReply.newBuilder().setClient(client).build());
                        response.onCompleted();
                    }

                    @Override
                    public void listClients(
                            ListClientsRequest request,
                            StreamObserver<ListClientsReply> response
                    ) {
                        listRequest.set(request);

                        response.onNext(ListClientsReply.newBuilder()
                                .setPage(request.getPage())
                                .setPageSize(request.getPageSize())
                                .setTotalElements(1)
                                .setTotalPages(1)
                                .build());
                        response.onCompleted();
                    }
                })
                .build()
                .start();

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("127.0.0.1", server.getPort())
                .usePlaintext()
                .build();

        GrpcChannelFactory channels = mock(GrpcChannelFactory.class);
        when(channels.createChannel("cases")).thenReturn(channel);
        ClientController controller = new ClientController(channels);

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("test-user")
                .build();

        try {
            var created = controller.create(
                    new ClientController.ClientInput("Fatima", "f@example.com", null, null),
                    jwt
            );

            assertEquals("client-123", created.id());
            assertEquals("Fatima", created.name());
            assertEquals("Fatima", createdRequest.get().getName());
            assertEquals("Bearer test-token", authorization.get());

            var page = controller.list(2, 10, "ACTIVE", jwt);

            assertEquals(2, page.page());
            assertEquals(10, page.pageSize());
            assertEquals(1, page.totalElements());
            assertEquals("ACTIVE", listRequest.get().getStatus());

            var badRequest = assertThrows(ResponseStatusException.class,
                    () -> controller.create(
                            new ClientController.ClientInput(" ", null, null, null), jwt
                    ));
            assertEquals(HttpStatus.BAD_REQUEST, badRequest.getStatusCode());

            var forbidden = assertThrows(ResponseStatusException.class,
                    () -> controller.create(
                            new ClientController.ClientInput("denied", null, null, null), jwt
                    ));
            assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }
}