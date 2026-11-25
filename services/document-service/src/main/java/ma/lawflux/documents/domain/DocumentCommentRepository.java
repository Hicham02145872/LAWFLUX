package ma.lawflux.documents.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface DocumentCommentRepository {

    DocumentComment save(DocumentComment comment);

    /** Du plus ancien au plus récent, limité au cabinet et aux visibilités demandées. */
    List<DocumentComment> findByDocument(UUID cabinetId, UUID documentId, Set<CommentVisibility> visibilities);
}