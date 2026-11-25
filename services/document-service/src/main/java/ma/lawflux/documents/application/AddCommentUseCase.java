package ma.lawflux.documents.application;

import java.time.Instant;
import java.util.UUID;
import ma.lawflux.documents.domain.CommentVisibility;
import ma.lawflux.documents.domain.DocumentAccessDeniedException;
import ma.lawflux.documents.domain.DocumentComment;
import ma.lawflux.documents.domain.DocumentCommentRepository;
import ma.lawflux.documents.domain.DocumentShareRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class AddCommentUseCase {

    private final DocumentShareRepository documents;
    private final DocumentCommentRepository comments;
    private final CommentAccessPolicy policy;

    public AddCommentUseCase(DocumentShareRepository documents, DocumentCommentRepository comments,
                             CommentAccessPolicy policy) {
        this.documents = documents;
        this.comments = comments;
        this.policy = policy;
    }

    public DocumentComment execute(CallerContext caller, UUID documentId,
                                   CommentVisibility visibility, String body) {
        // Le texte est validé avant toute lecture ou écriture.
        String text = CommentRules.validBody(body);
        CommentRules.Loaded loaded = CommentRules.load(documents, policy, caller, documentId);

        CommentRight required = visibility == CommentVisibility.INTERNAL
                ? CommentRight.WRITE_INTERNAL : CommentRight.WRITE_SHARED;
        if (!loaded.rights().contains(required)) {
            throw new DocumentAccessDeniedException("Action non autorisée");
        }
        return comments.save(new DocumentComment(UUID.randomUUID(), documentId, caller.cabinetId(),
                caller.userId(), caller.authorName(), visibility, text, Instant.now()));
    }
}