package ma.lawflux.cases.api.grpc;
import ma.lawflux.contracts.cases.v1.CaseServiceGrpc;
import org.springframework.stereotype.Service;
/** Contrat réservé. Implémenter les cas d'utilisation après l'authentification. */
@Service
public class CaseServiceEndpoint extends CaseServiceGrpc.CaseServiceImplBase {}
