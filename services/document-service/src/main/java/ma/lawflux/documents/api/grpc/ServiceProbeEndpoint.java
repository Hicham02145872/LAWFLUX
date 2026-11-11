package ma.lawflux.documents.api.grpc;
import io.grpc.stub.StreamObserver;
import ma.lawflux.contracts.platform.v1.*;
import ma.lawflux.platform.grpc.ScaffoldGrpcBoundary;
import org.springframework.stereotype.Service;
@Service
public class ServiceProbeEndpoint extends ServiceProbeGrpc.ServiceProbeImplBase {
    @Override
    public void ping(ProbeRequest request, StreamObserver<ProbeReply> observer) {
        String correlation = ScaffoldGrpcBoundary.CORRELATION_CONTEXT.get();
        observer.onNext(ProbeReply.newBuilder().setService("document-service").setState("SCAFFOLD_READY")
            .setCorrelationId(correlation == null ? "" : correlation).build());
        observer.onCompleted();
    }
}
