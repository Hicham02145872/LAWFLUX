package ma.lawflux.documents.api.grpc;

import ma.lawflux.contracts.documents.v1.DocumentServiceGrpc;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Configuration par défaut : aucune méthode métier n'est implémentée,
 * tout appel renvoie UNIMPLEMENTED (et la sécurité gRPC commune le refuse déjà).
 */
@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled",
        havingValue = "false", matchIfMissing = true)
public class DisabledDocumentServiceEndpoint extends DocumentServiceGrpc.DocumentServiceImplBase {
}