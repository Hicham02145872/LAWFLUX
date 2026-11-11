package ma.lawflux.platform;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import ma.lawflux.contracts.cases.v1.CaseServiceGrpc;
import ma.lawflux.contracts.cases.v1.GetCaseRequest;
import ma.lawflux.contracts.platform.v1.*;
import ma.lawflux.platform.grpc.ScaffoldGrpcBoundary;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ScaffoldBoundaryTest {
    @Test void probeCarriesCorrelationWhileBusinessRpcRemainsClosed() throws Exception {
        String name = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(name).directExecutor()
            .intercept(new ScaffoldGrpcBoundary())
            .addService(new ServiceProbeGrpc.ServiceProbeImplBase() {
                @Override public void ping(ProbeRequest request, StreamObserver<ProbeReply> observer) {
                    observer.onNext(ProbeReply.newBuilder().setService("test-service")
                        .setCorrelationId(ScaffoldGrpcBoundary.CORRELATION_CONTEXT.get()).build());
                    observer.onCompleted();
                }
            }).addService(new CaseServiceGrpc.CaseServiceImplBase() {}).build().start();
        ManagedChannel channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        try {
            Metadata metadata = new Metadata();
            metadata.put(ScaffoldGrpcBoundary.CORRELATION, "correlation-test-42");
            var reply = ServiceProbeGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).ping(ProbeRequest.getDefaultInstance());
            assertThat(reply.getCorrelationId()).isEqualTo("correlation-test-42");
            assertThatThrownBy(() -> CaseServiceGrpc.newBlockingStub(channel)
                .getCase(GetCaseRequest.newBuilder().setCaseId("any-case").build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                    e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
        } finally { channel.shutdownNow(); server.shutdownNow(); }
    }
}
