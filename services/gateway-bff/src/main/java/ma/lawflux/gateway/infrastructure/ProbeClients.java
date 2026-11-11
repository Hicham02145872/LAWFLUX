package ma.lawflux.gateway.infrastructure;
import java.util.Map;
import ma.lawflux.contracts.platform.v1.ServiceProbeGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;
@Configuration(proxyBeanMethods = false)
public class ProbeClients {
    @Bean
    Map<String, ServiceProbeGrpc.ServiceProbeBlockingStub> probeStubs(GrpcChannelFactory channels) {
        return Map.of(
            "identity", ServiceProbeGrpc.newBlockingStub(channels.createChannel("identity")),
            "cases", ServiceProbeGrpc.newBlockingStub(channels.createChannel("cases")),
            "documents", ServiceProbeGrpc.newBlockingStub(channels.createChannel("documents")),
            "billing", ServiceProbeGrpc.newBlockingStub(channels.createChannel("billing")));
    }
}
