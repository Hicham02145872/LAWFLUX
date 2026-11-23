package ma.lawflux.cases.api.grpc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.oauth2.jwt.Jwt;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import ma.lawflux.cases.application.CaseAccessService;
import ma.lawflux.cases.application.ClientService;
import ma.lawflux.cases.domain.AccessKind;
import ma.lawflux.cases.domain.CaseAccessRepository;
import ma.lawflux.cases.infrastructure.CaseJwtIdentity;
import ma.lawflux.contracts.cases.v1.CaseAccessGrantReply;
import ma.lawflux.contracts.cases.v1.CheckCaseAccessReply;
import ma.lawflux.contracts.cases.v1.CheckCaseAccessRequest;
import ma.lawflux.contracts.cases.v1.GrantCaseAccessRequest;
import ma.lawflux.platform.grpc.ScaffoldGrpcBoundary;

class CaseServiceEndpointTest {
    private final UUID cabinet = UUID.randomUUID();
    private final UUID caseId = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private final CaseAccessRepository repository = mock(CaseAccessRepository.class);
    private final Jwt lawyerJwt = Jwt.withTokenValue("valid")
            .header("alg", "RS256")
            .subject(user.toString()).claim("cabinet_id", cabinet.toString())
            .claim("realm_access", Map.of("roles", List.of("lawyer"))).build();
    private final CaseServiceEndpoint endpoint = new CaseServiceEndpoint(
        new CaseAccessService(repository),
        mock(ClientService.class),
        new CaseJwtIdentity(token -> lawyerJwt));

    @Test
    void bearerIdentityAndActiveAssignmentAreRequired() {
        when(repository.caseExists(cabinet, caseId)).thenReturn(true);
        when(repository.hasActiveGrant(cabinet, caseId, user, AccessKind.STAFF)).thenReturn(true);
        var request = CheckCaseAccessRequest.newBuilder()
                .setCaseId(caseId.toString()).setAction("DOCUMENT_READ").build();

        Recorder<CheckCaseAccessReply> anonymous = new Recorder<>();
        endpoint.checkCaseAccess(request, anonymous);
        assertEquals(Status.Code.UNAUTHENTICATED, Status.fromThrowable(anonymous.error.get()).getCode());

        Recorder<CheckCaseAccessReply> assigned = withBearer(() -> {
            Recorder<CheckCaseAccessReply> response = new Recorder<>();
            endpoint.checkCaseAccess(request, response);
            return response;
        });
        assertTrue(assigned.value.get().getAllowed());

        Recorder<CheckCaseAccessReply> foreign = withBearer(() -> {
            Recorder<CheckCaseAccessReply> response = new Recorder<>();
            endpoint.checkCaseAccess(CheckCaseAccessRequest.newBuilder()
                    .setCaseId(UUID.randomUUID().toString()).setAction("DOCUMENT_READ").build(), response);
            return response;
        });
        assertFalse(foreign.value.get().getAllowed());
    }

    @Test
    void lawyerCannotGrantAccess() {
        when(repository.caseExists(cabinet, caseId)).thenReturn(true);
        Recorder<CaseAccessGrantReply> response = withBearer(() -> {
            Recorder<CaseAccessGrantReply> result = new Recorder<>();
            endpoint.grantCaseAccess(GrantCaseAccessRequest.newBuilder()
                    .setCaseId(caseId.toString()).setUserId(UUID.randomUUID().toString())
                    .setKind("CLIENT").build(), result);
            return result;
        });
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(response.error.get()).getCode());
        verify(repository, never()).grant(any(), any(), any(), any(), any());
    }

    private static <T> T withBearer(java.util.function.Supplier<T> action) {
        Context context = Context.current().withValue(ScaffoldGrpcBoundary.AUTHORIZATION_CONTEXT, "Bearer valid");
        Context previous = context.attach();
        try { return action.get(); }
        finally { context.detach(previous); }
    }

    private static class Recorder<T> implements StreamObserver<T> {
        final AtomicReference<T> value = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        @Override public void onNext(T v) { value.set(v); }
        @Override public void onError(Throwable t) { error.set(t); }
        @Override public void onCompleted() { }
    }
}
