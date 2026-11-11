package ma.lawflux.gateway;
import java.util.Map;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import ma.lawflux.gateway.api.SystemController;
import ma.lawflux.platform.grpc.ScaffoldGrpcBoundary;
import ma.lawflux.platform.web.CorrelationFilter;
import ma.lawflux.contracts.platform.v1.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SystemControllerTest {
    @Test void httpReachesGrpcAndPreservesCorrelation() throws Exception {
        Server server = ServerBuilder.forPort(0).intercept(new ScaffoldGrpcBoundary())
            .addService(new ServiceProbeGrpc.ServiceProbeImplBase() {
                @Override public void ping(ProbeRequest req, StreamObserver<ProbeReply> obs) {
                    obs.onNext(ProbeReply.newBuilder().setService("case-service").setState("SCAFFOLD_READY")
                        .setCorrelationId(ScaffoldGrpcBoundary.CORRELATION_CONTEXT.get()).build());
                    obs.onCompleted();
                }
            }).build().start();
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", server.getPort()).usePlaintext().build();
        try {
            var mvc = MockMvcBuilders.standaloneSetup(new SystemController(Map.of("cases", ServiceProbeGrpc.newBlockingStub(channel))))
                .addFilters(new CorrelationFilter()).build();
            mvc.perform(get("/api/system/services/cases").header("X-Correlation-Id", "demo-42"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.service").value("case-service"))
                .andExpect(jsonPath("$.correlationId").value("demo-42"));
            mvc.perform(get("/api/system/services/unknown")).andExpect(status().isNotFound());
            server.shutdownNow().awaitTermination();
            mvc.perform(get("/api/system/services/cases")).andExpect(status().isServiceUnavailable());
        } finally { channel.shutdownNow(); server.shutdownNow(); }
    }
}
