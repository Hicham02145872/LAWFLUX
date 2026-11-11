package ma.lawflux.gateway.api;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import ma.lawflux.contracts.platform.v1.*;
import ma.lawflux.platform.grpc.ScaffoldGrpcBoundary;
import ma.lawflux.platform.web.CorrelationFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/system/services")
public class SystemController {
    private final Map<String, ServiceProbeGrpc.ServiceProbeBlockingStub> clients;
    public SystemController(Map<String, ServiceProbeGrpc.ServiceProbeBlockingStub> probeStubs) { this.clients = probeStubs; }
    @GetMapping("/{name}")
    public ProbeView probe(@PathVariable String name) {
        var client = clients.get(name);
        if (client == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service inconnu");
        Metadata headers = new Metadata();
        headers.put(ScaffoldGrpcBoundary.CORRELATION, CorrelationFilter.normalize(MDC.get("correlationId")));
        try {
            var reply = client.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                    .withDeadlineAfter(1500, TimeUnit.MILLISECONDS).ping(ProbeRequest.getDefaultInstance());
            return new ProbeView(reply.getService(), reply.getState(), reply.getCorrelationId());
        } catch (StatusRuntimeException e) {
            HttpStatus status = e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE;
            throw new ResponseStatusException(status, "Service temporairement indisponible");
        }
    }
    public record ProbeView(String service, String state, String correlationId) {}
}
