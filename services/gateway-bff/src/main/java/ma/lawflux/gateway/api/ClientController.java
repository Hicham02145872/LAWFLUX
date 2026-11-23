package ma.lawflux.gateway.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import ma.lawflux.contracts.cases.v1.ArchiveClientRequest;
import ma.lawflux.contracts.cases.v1.CaseServiceGrpc;
import ma.lawflux.contracts.cases.v1.ClientRecord;
import ma.lawflux.contracts.cases.v1.CreateClientRequest;
import ma.lawflux.contracts.cases.v1.GetClientRequest;
import ma.lawflux.contracts.cases.v1.ListClientsReply;
import ma.lawflux.contracts.cases.v1.ListClientsRequest;
import ma.lawflux.contracts.cases.v1.UpdateClientRequest;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
    private final CaseServiceGrpc.CaseServiceBlockingStub cases;

    public ClientController(GrpcChannelFactory channels) {
        cases = CaseServiceGrpc.newBlockingStub(channels.createChannel("cases"));
    }

    @GetMapping
    public ClientPageView list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            ListClientsReply reply = authenticated(jwt).listClients(
                    ListClientsRequest.newBuilder()
                            .setStatus(status == null ? "" : status)
                            .setPage(page)
                            .setPageSize(pageSize)
                            .build()
            );

            List<ClientView> items = reply.getClientsList().stream()
                    .map(ClientController::view)
                    .toList();

            return new ClientPageView(
                    items,
                    reply.getPage(),
                    reply.getPageSize(),
                    reply.getTotalElements(),
                    reply.getTotalPages()
            );
        } catch (StatusRuntimeException e) {
            throw httpError(e);
        }
    }

    @PostMapping
    public ClientView create(
            @RequestBody ClientInput input,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (input == null || input.name() == null || input.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        }

        try {
            ClientRecord client = authenticated(jwt).createClient(
                    CreateClientRequest.newBuilder()
                            .setName(input.name())
                            .setEmail(input.email() == null ? "" : input.email())
                            .setPhone(input.phone() == null ? "" : input.phone())
                            .setAddress(input.address() == null ? "" : input.address())
                            .build()
            ).getClient();

            return view(client);
        } catch (StatusRuntimeException e) {
            throw httpError(e);
        }
    }

    @GetMapping("/{clientId}")
    public ClientView get(
            @PathVariable String clientId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            ClientRecord client = authenticated(jwt).getClient(
                    GetClientRequest.newBuilder().setClientId(clientId).build()
            ).getClient();

            return view(client);
        } catch (StatusRuntimeException e) {
            throw httpError(e);
        }
    }

    @PutMapping("/{clientId}")
    public ClientView update(
            @PathVariable String clientId,
            @RequestBody ClientInput input,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (input == null || input.name() == null || input.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        }

        try {
            ClientRecord client = authenticated(jwt).updateClient(
                    UpdateClientRequest.newBuilder()
                            .setClientId(clientId)
                            .setName(input.name())
                            .setEmail(input.email() == null ? "" : input.email())
                            .setPhone(input.phone() == null ? "" : input.phone())
                            .setAddress(input.address() == null ? "" : input.address())
                            .build()
            ).getClient();

            return view(client);
        } catch (StatusRuntimeException e) {
            throw httpError(e);
        }
    }

    @PatchMapping("/{clientId}/archive")
    public ArchiveView archive(
            @PathVariable String clientId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            boolean archived = authenticated(jwt).archiveClient(
                    ArchiveClientRequest.newBuilder().setClientId(clientId).build()
            ).getArchived();

            return new ArchiveView(archived);
        } catch (StatusRuntimeException e) {
            throw httpError(e);
        }
    }

    private CaseServiceGrpc.CaseServiceBlockingStub authenticated(Jwt jwt) {
        Metadata headers = new Metadata();
        headers.put(
                Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
                "Bearer " + jwt.getTokenValue()
        );

        return cases.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    private static ClientView view(ClientRecord client) {
        return new ClientView(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getAddress(),
                client.getStatus(),
                client.getCreatedAt(),
                client.getUpdatedAt(),
                client.getArchivedAt()
        );
    }

    private static ResponseStatusException httpError(StatusRuntimeException e) {
        HttpStatus status = switch (e.getStatus().getCode()) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAVAILABLE, DEADLINE_EXCEEDED -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };

        return new ResponseStatusException(status, e.getStatus().getDescription());
    }

    public record ClientInput(String name, String email, String phone, String address) {}

    public record ClientView(
            String id,
            String name,
            String email,
            String phone,
            String address,
            String status,
            String createdAt,
            String updatedAt,
            String archivedAt
    ) {}

    public record ClientPageView(
            List<ClientView> items,
            int page,
            int pageSize,
            long totalElements,
            long totalPages
    ) {}

    public record ArchiveView(boolean archived) {}
}