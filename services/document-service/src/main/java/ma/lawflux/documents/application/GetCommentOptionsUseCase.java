package ma.lawflux.documents.application;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import ma.lawflux.documents.domain.CommentVisibility;
import ma.lawflux.documents.domain.DocumentShareRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class GetCommentOptionsUseCase {

    private final DocumentShareRepository documents;
    private final CommentAccessPolicy policy;

    public GetCommentOptionsUseCase(DocumentShareRepository documents, CommentAccessPolicy policy) {
        this.documents = documents;
        this.policy = policy;
    }

    /** Visibilités que l'appelant peut choisir en écrivant ; ensemble vide = lecture seule. */
    public Set<CommentVisibility> execute(CallerContext caller, UUID documentId) {
        CommentRules.Loaded loaded = CommentRules.load(documents, policy, caller, documentId);
        EnumSet<CommentVisibility> options = EnumSet.noneOf(CommentVisibility.class);
        if (loaded.rights().contains(CommentRight.WRITE_INTERNAL)) options.add(CommentVisibility.INTERNAL);
        if (loaded.rights().contains(CommentRight.WRITE_SHARED)) options.add(CommentVisibility.SHARED);
        return options;
    }
}