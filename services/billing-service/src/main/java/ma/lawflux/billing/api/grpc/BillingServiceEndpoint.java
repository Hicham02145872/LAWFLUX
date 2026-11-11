package ma.lawflux.billing.api.grpc;
import ma.lawflux.contracts.billing.v1.BillingServiceGrpc;
import org.springframework.stereotype.Service;
/** Contrat réservé. Implémenter les cas d'utilisation après l'authentification. */
@Service
public class BillingServiceEndpoint extends BillingServiceGrpc.BillingServiceImplBase {}
