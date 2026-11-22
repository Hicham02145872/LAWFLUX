package ma.lawflux.documents.application;

import java.util.UUID;
import ma.lawflux.documents.domain.DocumentShareRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class GetSharingUseCase {

    private final DocumentShareRepository shares;
    private final DocumentAccessPolicy policy;

    public GetSharingUseCase(DocumentShareRepository shares, DocumentAccessPolicy policy) {
        this.shares = shares;
        this.policy = policy;
    }

    public SharingStatus execute(CallerContext caller, UUID documentId) {
        SharingRules.Loaded loaded = SharingRules.load(shares, policy, caller, documentId);
        return new SharingStatus(loaded.document(), loaded.shared(),
                SharingRules.applicable(loaded.rights(), loaded.shared()));
    }
}