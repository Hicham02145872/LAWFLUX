package ma.lawflux.cases.api.grpc;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import ma.lawflux.cases.application.CaseAccessService;
import ma.lawflux.cases.application.CaseCaller;
import ma.lawflux.cases.application.ClientService;
import ma.lawflux.cases.domain.AccessKind;
import ma.lawflux.cases.domain.Client;
import ma.lawflux.cases.domain.ClientStatus;
import ma.lawflux.cases.infrastructure.CaseJwtIdentity;
import ma.lawflux.contracts.cases.v1.ArchiveClientReply;
import ma.lawflux.contracts.cases.v1.ArchiveClientRequest;
import ma.lawflux.contracts.cases.v1.CaseAccessGrant;
import ma.lawflux.contracts.cases.v1.CaseAccessGrantReply;
import ma.lawflux.contracts.cases.v1.CaseServiceGrpc;
import ma.lawflux.contracts.cases.v1.CheckCaseAccessReply;
import ma.lawflux.contracts.cases.v1.CheckCaseAccessRequest;
import ma.lawflux.contracts.cases.v1.ClientRecord;
import ma.lawflux.contracts.cases.v1.ClientReply;
import ma.lawflux.contracts.cases.v1.CreateClientRequest;
import ma.lawflux.contracts.cases.v1.GetClientRequest;
import ma.lawflux.contracts.cases.v1.GrantCaseAccessRequest;
import ma.lawflux.contracts.cases.v1.ListCaseAccessReply;
import ma.lawflux.contracts.cases.v1.ListCaseAccessRequest;
import ma.lawflux.contracts.cases.v1.ListClientsReply;
import ma.lawflux.contracts.cases.v1.ListClientsRequest;
import ma.lawflux.contracts.cases.v1.RevokeCaseAccessRequest;
import ma.lawflux.contracts.cases.v1.UpdateClientRequest;

@Service
public class CaseServiceEndpoint extends CaseServiceGrpc.CaseServiceImplBase {
    private final CaseAccessService access;
    private final ClientService clients;
    private final CaseJwtIdentity identity;

    public CaseServiceEndpoint(
            CaseAccessService access,
            ClientService clients,
            CaseJwtIdentity identity
    ) {
        this.access = access;
        this.clients = clients;
        this.identity = identity;
    }

    @Override
    public void checkCaseAccess(
            CheckCaseAccessRequest request,
            StreamObserver<CheckCaseAccessReply> response
    ) {
        respond(response, () -> {
            CaseCaller caller = identity.current();
            UUID caseId = parseOptional(request.getCaseId());
            return CheckCaseAccessReply.newBuilder()
                    .setAllowed(access.allowed(caller, caseId, request.getAction()))
                    .build();
        });
    }

    @Override
    public void listCaseAccess(
            ListCaseAccessRequest request,
            StreamObserver<ListCaseAccessReply> response
    ) {
        respond(response, () -> {
            CaseCaller caller = identity.current();
            var grants = access.grants(caller, parseRequired(request.getCaseId()));
            var reply = ListCaseAccessReply.newBuilder();

            grants.forEach(grant -> reply.addGrants(CaseAccessGrant.newBuilder()
                    .setUserId(grant.userId().toString())
                    .setKind(grant.kind().name())
                    .setGrantedAt(grant.grantedAt().toString())
                    .build()));

            return reply.build();
        });
    }

    @Override
    public void grantCaseAccess(
            GrantCaseAccessRequest request,
            StreamObserver<CaseAccessGrantReply> response
    ) {
        respond(response, () -> {
            access.grant(
                    identity.current(),
                    parseRequired(request.getCaseId()),
                    parseRequired(request.getUserId()),
                    parseKind(request.getKind())
            );
            return CaseAccessGrantReply.newBuilder().setActive(true).build();
        });
    }

    @Override
    public void revokeCaseAccess(
            RevokeCaseAccessRequest request,
            StreamObserver<CaseAccessGrantReply> response
    ) {
        respond(response, () -> {
            access.revoke(
                    identity.current(),
                    parseRequired(request.getCaseId()),
                    parseRequired(request.getUserId()),
                    parseKind(request.getKind())
            );
            return CaseAccessGrantReply.newBuilder().setActive(false).build();
        });
    }

    @Override
    public void createClient(
            CreateClientRequest request,
            StreamObserver<ClientReply> response
    ) {
        respond(response, () -> clientReply(clients.create(
                identity.current(),
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getAddress()
        )));
    }

    @Override
    public void getClient(
            GetClientRequest request,
            StreamObserver<ClientReply> response
    ) {
        respond(response, () -> clientReply(clients.get(
                identity.current(),
                parseRequired(request.getClientId())
        )));
    }

    @Override
    public void updateClient(
            UpdateClientRequest request,
            StreamObserver<ClientReply> response
    ) {
        respond(response, () -> clientReply(clients.update(
                identity.current(),
                parseRequired(request.getClientId()),
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getAddress()
        )));
    }

    @Override
    public void archiveClient(
            ArchiveClientRequest request,
            StreamObserver<ArchiveClientReply> response
    ) {
        respond(response, () -> {
            clients.archive(identity.current(), parseRequired(request.getClientId()));
            return ArchiveClientReply.newBuilder().setArchived(true).build();
        });
    }

    @Override
    public void listClients(
            ListClientsRequest request,
            StreamObserver<ListClientsReply> response
    ) {
        respond(response, () -> {
            int page = request.getPage() == 0 ? 1 : request.getPage();
            int pageSize = request.getPageSize() == 0 ? 20 : request.getPageSize();

            var result = clients.list(
                    identity.current(),
                    parseClientStatus(request.getStatus()),
                    page,
                    pageSize
            );

            var reply = ListClientsReply.newBuilder()
                    .setPage(result.page())
                    .setPageSize(result.pageSize())
                    .setTotalElements(result.totalElements())
                    .setTotalPages(result.totalPages());

            result.items().stream()
                    .map(CaseServiceEndpoint::toClientRecord)
                    .forEach(reply::addClients);

            return reply.build();
        });
    }

    private static ClientReply clientReply(Client client) {
        return ClientReply.newBuilder()
                .setClient(toClientRecord(client))
                .build();
    }

    private static ClientRecord toClientRecord(Client client) {
        ClientRecord.Builder record = ClientRecord.newBuilder()
                .setId(client.id().toString())
                .setName(client.name())
                .setEmail(client.email() == null ? "" : client.email())
                .setPhone(client.phone() == null ? "" : client.phone())
                .setAddress(client.address() == null ? "" : client.address())
                .setStatus(client.status().name())
                .setCreatedAt(client.createdAt().toString())
                .setUpdatedAt(client.updatedAt().toString());

        if (client.archivedAt() != null) {
            record.setArchivedAt(client.archivedAt().toString());
        }

        return record.build();
    }

    private static ClientStatus parseClientStatus(String value) {
        if (value == null || value.isBlank()) return null;

        try {
            return ClientStatus.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut client invalide");
        }
    }

    private static UUID parseOptional(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private static UUID parseRequired(String value) {
        UUID id = parseOptional(value);
        if (id == null) throw new IllegalArgumentException("Identifiant invalide");
        return id;
    }

    private static AccessKind parseKind(String value) {
        try {
            return AccessKind.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Type d'accès invalide");
        }
    }

    private static <T> void respond(
            StreamObserver<T> response,
            Supplier<T> operation
    ) {
        try {
            response.onNext(operation.get());
            response.onCompleted();
        } catch (RuntimeException e) {
            Status status = e instanceof StatusRuntimeException grpc
                    ? grpc.getStatus()
                    : e instanceof SecurityException ? Status.PERMISSION_DENIED
                    : e instanceof NoSuchElementException ? Status.NOT_FOUND
                    : e instanceof IllegalArgumentException ? Status.INVALID_ARGUMENT
                    : Status.INTERNAL;

            response.onError(status.withDescription(
                    status == Status.INTERNAL ? "Erreur interne" : e.getMessage()
            ).asRuntimeException());
        }
    }
}