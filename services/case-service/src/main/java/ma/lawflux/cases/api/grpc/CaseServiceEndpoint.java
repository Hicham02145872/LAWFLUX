package ma.lawflux.cases.api.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Supplier;
import ma.lawflux.cases.application.CaseAccessService;
import ma.lawflux.cases.domain.AccessKind;
import ma.lawflux.cases.infrastructure.CaseJwtIdentity;
import ma.lawflux.contracts.cases.v1.CaseAccessGrant;
import ma.lawflux.contracts.cases.v1.CaseAccessGrantReply;
import ma.lawflux.contracts.cases.v1.CaseServiceGrpc;
import ma.lawflux.contracts.cases.v1.CheckCaseAccessReply;
import ma.lawflux.contracts.cases.v1.CheckCaseAccessRequest;
import ma.lawflux.contracts.cases.v1.GrantCaseAccessRequest;
import ma.lawflux.contracts.cases.v1.ListCaseAccessReply;
import ma.lawflux.contracts.cases.v1.ListCaseAccessRequest;
import ma.lawflux.contracts.cases.v1.RevokeCaseAccessRequest;
import org.springframework.stereotype.Service;

@Service
public class CaseServiceEndpoint extends CaseServiceGrpc.CaseServiceImplBase {
    private final CaseAccessService access;
    private final CaseJwtIdentity identity;

    public CaseServiceEndpoint(CaseAccessService access, CaseJwtIdentity identity) {
        this.access = access;
        this.identity = identity;
    }

    @Override
    public void checkCaseAccess(CheckCaseAccessRequest request, StreamObserver<CheckCaseAccessReply> response) {
        respond(response, () -> {
            var caller = identity.current();
            UUID caseId = parseOptional(request.getCaseId());
            return CheckCaseAccessReply.newBuilder()
                    .setAllowed(access.allowed(caller, caseId, request.getAction())).build();
        });
    }

    @Override
    public void listCaseAccess(ListCaseAccessRequest request, StreamObserver<ListCaseAccessReply> response) {
        respond(response, () -> {
            var caller = identity.current();
            var grants = access.grants(caller, parseRequired(request.getCaseId()));
            var reply = ListCaseAccessReply.newBuilder();
            grants.forEach(g -> reply.addGrants(CaseAccessGrant.newBuilder()
                    .setUserId(g.userId().toString()).setKind(g.kind().name())
                    .setGrantedAt(g.grantedAt().toString()).build()));
            return reply.build();
        });
    }

    @Override
    public void grantCaseAccess(GrantCaseAccessRequest request, StreamObserver<CaseAccessGrantReply> response) {
        respond(response, () -> {
            access.grant(identity.current(), parseRequired(request.getCaseId()),
                    parseRequired(request.getUserId()), parseKind(request.getKind()));
            return CaseAccessGrantReply.newBuilder().setActive(true).build();
        });
    }

    @Override
    public void revokeCaseAccess(RevokeCaseAccessRequest request, StreamObserver<CaseAccessGrantReply> response) {
        respond(response, () -> {
            access.revoke(identity.current(), parseRequired(request.getCaseId()),
                    parseRequired(request.getUserId()), parseKind(request.getKind()));
            return CaseAccessGrantReply.newBuilder().setActive(false).build();
        });
    }

    private static UUID parseOptional(String value) {
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException | NullPointerException e) { return null; }
    }

    private static UUID parseRequired(String value) {
        UUID id = parseOptional(value);
        if (id == null) throw new IllegalArgumentException("Identifiant invalide");
        return id;
    }

    private static AccessKind parseKind(String value) {
        try { return AccessKind.valueOf(value); }
        catch (IllegalArgumentException | NullPointerException e) { throw new IllegalArgumentException("Type d'accès invalide"); }
    }

    private static <T> void respond(StreamObserver<T> response, Supplier<T> operation) {
        try {
            response.onNext(operation.get());
            response.onCompleted();
        } catch (RuntimeException e) {
            Status status = e instanceof StatusRuntimeException grpc ? grpc.getStatus()
                    : e instanceof SecurityException ? Status.PERMISSION_DENIED
                    : e instanceof NoSuchElementException ? Status.NOT_FOUND
                    : e instanceof IllegalArgumentException ? Status.INVALID_ARGUMENT
                    : Status.INTERNAL;
            response.onError(status.withDescription(status == Status.INTERNAL
                    ? "Erreur interne" : e.getMessage()).asRuntimeException());
        }
    }
}
