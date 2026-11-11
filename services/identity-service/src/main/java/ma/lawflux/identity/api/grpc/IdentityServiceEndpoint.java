package ma.lawflux.identity.api.grpc;
import ma.lawflux.contracts.identity.v1.IdentityServiceGrpc;
import org.springframework.stereotype.Service;
/** Contrat réservé. Implémenter les cas d'utilisation après l'authentification. */
@Service
public class IdentityServiceEndpoint extends IdentityServiceGrpc.IdentityServiceImplBase {}
