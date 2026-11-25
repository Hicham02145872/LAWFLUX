package ma.lawflux.documents.application;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import ma.lawflux.documents.domain.CommentVisibility;
import ma.lawflux.documents.domain.DocumentComment;
import ma.lawflux.documents.domain.DocumentCommentRepository;
import ma.lawflux.documents.domain.DocumentShareRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class ListCommentsUseCase {

    private final DocumentShareRepository documents;
    private final DocumentCommentRepository comments;
    private final CommentAccessPolicy policy;

    public ListCommentsUseCase(DocumentShareRepository documents, DocumentCommentRepository comments,
                               CommentAccessPolicy policy) {
        this.documents = documents;
        this.comments = comments;
        this.policy = policy;
    }

    public List<DocumentComment> execute(CallerContext caller, UUID documentId) {
        CommentRules.Loaded loaded = CommentRules.load(documents, policy, caller, documentId);

        // Seules les visibilités autorisées sont demandées à la base : un commentaire
        // interne n'est jamais lu pour un client, puis filtré.
        Set<CommentVisibility> visible = EnumSet.noneOf(CommentVisibility.class);
        if (loaded.rights().contains(CommentRight.READ_INTERNAL)) visible.add(CommentVisibility.INTERNAL);
        if (loaded.rights().contains(CommentRight.READ_SHARED)) visible.add(CommentVisibility.SHARED);
        if (visible.isEmpty()) {
            return List.of();
        }
        return comments.findByDocument(caller.cabinetId(), documentId, visible);
    }
}