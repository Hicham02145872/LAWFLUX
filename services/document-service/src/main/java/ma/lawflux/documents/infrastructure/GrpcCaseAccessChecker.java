package ma.lawflux.documents.infrastructure;

import io.grpc.Deadline;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import ma.lawflux.contracts.cases.v1.CaseServiceGrpc;
import ma.lawflux.contracts.cases.v1.CheckCaseAccessRequest;
import ma.lawflux.documents.application.CaseAccessChecker;
import ma.lawflux.platform.grpc.ScaffoldGrpcBoundary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class GrpcCaseAccessChecker implements CaseAccessChecker {
    private final CaseServiceGrpc.CaseServiceBlockingStub stub;

    public GrpcCaseAccessChecker(GrpcChannelFactory channels) {
        stub = CaseServiceGrpc.newBlockingStub(channels.createChannel("cases"));
    }

    @Override
    public boolean canAccess(UUID cabinetId, UUID caseId) {
        String authorization = ScaffoldGrpcBoundary.AUTHORIZATION_CONTEXT.get();
        if (authorization == null || caseId == null || cabinetId == null) return false;
        Metadata headers = new Metadata();
        headers.put(ScaffoldGrpcBoundary.AUTHORIZATION, authorization);
        try {
            return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                    .withDeadline(Deadline.after(2, TimeUnit.SECONDS))
                    .checkCaseAccess(CheckCaseAccessRequest.newBuilder()
                            .setCaseId(caseId.toString()).setAction("DOCUMENT_READ").build())
                    .getAllowed();
        } catch (StatusRuntimeException e) {
            return false;
        }
    }
}
