package ma.lawflux.documents.application;

import java.time.Instant;
import java.util.UUID;
import ma.lawflux.documents.domain.DocumentAction;
import ma.lawflux.documents.domain.DocumentShareRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class ShareDocumentUseCase {

    private final DocumentShareRepository shares;
    private final DocumentAccessPolicy policy;

    public ShareDocumentUseCase(DocumentShareRepository shares, DocumentAccessPolicy policy) {
        this.shares = shares;
        this.policy = policy;
    }

    public SharingStatus execute(CallerContext caller, UUID documentId) {
        SharingRules.Loaded loaded = SharingRules.load(shares, policy, caller, documentId);
        SharingRules.require(loaded, DocumentAction.SHARE);
        if (!loaded.shared()) {
            shares.share(caller.cabinetId(), documentId, caller.userId(), Instant.now());
        }
        return SharingRules.statusAfter(policy, caller, loaded.document(), true);
    }
}