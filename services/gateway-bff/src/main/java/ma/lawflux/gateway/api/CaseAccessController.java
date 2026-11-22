package ma.lawflux.gateway.api;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;
import ma.lawflux.contracts.cases.v1.CaseServiceGrpc;
import ma.lawflux.contracts.cases.v1.GrantCaseAccessRequest;
import ma.lawflux.contracts.cases.v1.ListCaseAccessRequest;
import ma.lawflux.contracts.cases.v1.RevokeCaseAccessRequest;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dossiers/{caseId}/acces")
public class CaseAccessController {
    private final CaseServiceGrpc.CaseServiceBlockingStub cases;

    public CaseAccessController(GrpcChannelFactory channels) {
        cases = CaseServiceGrpc.newBlockingStub(channels.createChannel("cases"));
    }

    @GetMapping
    public List<GrantView> list(@PathVariable String caseId, @AuthenticationPrincipal Jwt jwt) {
        try {
            return cases.withInterceptors(auth(jwt)).withDeadlineAfter(5, TimeUnit.SECONDS)
                    .listCaseAccess(ListCaseAccessRequest.newBuilder().setCaseId(caseId).build())
                    .getGrantsList().stream().map(g -> new GrantView(g.getUserId(), g.getKind(), g.getGrantedAt()))
                    .toList();
        } catch (StatusRuntimeException e) { throw httpError(e); }
    }

    @PostMapping
    public GrantState grant(@PathVariable String caseId, @RequestBody GrantInput input,
                            @AuthenticationPrincipal Jwt jwt) {
        if (input == null || input.userId() == null || input.kind() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Affectation invalide");
        try {
            boolean active = cases.withInterceptors(auth(jwt)).withDeadlineAfter(5, TimeUnit.SECONDS)
                    .grantCaseAccess(GrantCaseAccessRequest.newBuilder().setCaseId(caseId)
                            .setUserId(input.userId()).setKind(input.kind()).build()).getActive();
            return new GrantState(active);
        } catch (StatusRuntimeException e) { throw httpError(e); }
    }

    @DeleteMapping("/{userId}")
    public GrantState revoke(@PathVariable String caseId, @PathVariable String userId,
                             @RequestParam String kind, @AuthenticationPrincipal Jwt jwt) {
        try {
            boolean active = cases.withInterceptors(auth(jwt)).withDeadlineAfter(5, TimeUnit.SECONDS)
                    .revokeCaseAccess(RevokeCaseAccessRequest.newBuilder().setCaseId(caseId)
                            .setUserId(userId).setKind(kind).build()).getActive();
            return new GrantState(active);
        } catch (StatusRuntimeException e) { throw httpError(e); }
    }

    private static io.grpc.ClientInterceptor auth(Jwt jwt) {
        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
                "Bearer " + jwt.getTokenValue());
        return MetadataUtils.newAttachHeadersInterceptor(headers);
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

    public record GrantInput(String userId, String kind) {}
    public record GrantView(String userId, String kind, String grantedAt) {}
    public record GrantState(boolean active) {}
}
