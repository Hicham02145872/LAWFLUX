package ma.lawflux.platform.grpc;
import io.grpc.*;
import ma.lawflux.platform.web.CorrelationFilter;

public class ScaffoldGrpcBoundary implements ServerInterceptor {
    public static final Metadata.Key<String> CORRELATION = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    public static final Context.Key<String> CORRELATION_CONTEXT = Context.key("correlation-id");
    public static final Context.Key<String> AUTHORIZATION_CONTEXT = Context.key("authorization");
    public static final Context.Key<String> METHOD_CONTEXT = Context.key("grpc-method");
    private final boolean documentsEnabled;

    public ScaffoldGrpcBoundary() { this(false); }
    public ScaffoldGrpcBoundary(boolean documentsEnabled) { this.documentsEnabled = documentsEnabled; }
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String method = call.getMethodDescriptor().getFullMethodName();
        boolean documentMethod = method.startsWith("lawflux.documents.v1.DocumentService/")
                || method.startsWith("lawflux.documents.v1.DocumentSharingService/");
        if (!method.equals("lawflux.platform.v1.ServiceProbe/Ping") && !(documentsEnabled && documentMethod)) {
            call.close(Status.UNAUTHENTICATED.withDescription("API métier fermée : authentification et autorisations à implémenter."), new Metadata());
            return new ServerCall.Listener<>() {};
        }
        Context context = Context.current().withValue(CORRELATION_CONTEXT,
                CorrelationFilter.normalize(headers.get(CORRELATION)))
                .withValue(AUTHORIZATION_CONTEXT, headers.get(AUTHORIZATION))
                .withValue(METHOD_CONTEXT, method);
        return Contexts.interceptCall(context, call, headers, next);
    }
}
