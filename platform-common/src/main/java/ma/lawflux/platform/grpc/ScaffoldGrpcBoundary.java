package ma.lawflux.platform.grpc;
import io.grpc.*;
import ma.lawflux.platform.web.CorrelationFilter;

public class ScaffoldGrpcBoundary implements ServerInterceptor {
    public static final Metadata.Key<String> CORRELATION = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Context.Key<String> CORRELATION_CONTEXT = Context.key("correlation-id");
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        if (!call.getMethodDescriptor().getFullMethodName().equals("lawflux.platform.v1.ServiceProbe/Ping")) {
            call.close(Status.UNAUTHENTICATED.withDescription("API métier fermée : authentification et autorisations à implémenter."), new Metadata());
            return new ServerCall.Listener<>() {};
        }
        Context context = Context.current().withValue(CORRELATION_CONTEXT,
                CorrelationFilter.normalize(headers.get(CORRELATION)));
        return Contexts.interceptCall(context, call, headers, next);
    }
}
