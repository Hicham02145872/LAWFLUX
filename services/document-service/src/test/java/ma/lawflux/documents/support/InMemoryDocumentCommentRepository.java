package ma.lawflux.documents.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import ma.lawflux.documents.domain.CommentVisibility;
import ma.lawflux.documents.domain.DocumentComment;
import ma.lawflux.documents.domain.DocumentCommentRepository;

public class InMemoryDocumentCommentRepository implements DocumentCommentRepository {

    public final List<DocumentComment> comments = new ArrayList<>();
    public final List<Set<CommentVisibility>> requestedVisibilities = new ArrayList<>();

    @Override
    public DocumentComment save(DocumentComment comment) {
        comments.add(comment);
        return comment;
    }

    @Override
    public List<DocumentComment> findByDocument(UUID cabinetId, UUID documentId,
                                                Set<CommentVisibility> visibilities) {
        requestedVisibilities.add(visibilities);
        return comments.stream()
                .filter(c -> c.cabinetId().equals(cabinetId) && c.documentId().equals(documentId))
                .filter(c -> visibilities.contains(c.visibility()))
                .sorted(Comparator.comparing(DocumentComment::createdAt))
                .toList();
    }
}