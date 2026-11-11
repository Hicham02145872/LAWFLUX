package ma.lawflux.documents.api.grpc;
import ma.lawflux.contracts.documents.v1.DocumentServiceGrpc;
import org.springframework.stereotype.Service;
/** Contrat réservé. Implémenter les cas d'utilisation après l'authentification. */
@Service
public class DocumentServiceEndpoint extends DocumentServiceGrpc.DocumentServiceImplBase {}
