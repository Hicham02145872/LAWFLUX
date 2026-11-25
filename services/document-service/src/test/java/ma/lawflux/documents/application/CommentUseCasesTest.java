package ma.lawflux.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import ma.lawflux.documents.domain.CommentVisibility;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentAccessDeniedException;
import ma.lawflux.documents.domain.DocumentComment;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.InvalidDocumentException;
import ma.lawflux.documents.support.InMemoryDocumentCommentRepository;
import ma.lawflux.documents.support.InMemoryDocumentShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentUseCasesTest {

    private final UUID cabinetId = UUID.randomUUID();
    private final UUID otherCabinetId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();
    private final UUID lawyerId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final UUID readerId = UUID.randomUUID();

    private final CallerContext lawyer = new CallerContext(lawyerId, cabinetId, "Karim");
    private final CallerContext client = new CallerContext(clientId, cabinetId, "Client SA");
    private final CallerContext reader = new CallerContext(readerId, cabinetId, "Lecteur");
    private final CallerContext foreign =
            new CallerContext(UUID.randomUUID(), otherCabinetId, "Autre cabinet");

    private final InMemoryDocumentShareRepository documents = new InMemoryDocumentShareRepository();
    private final InMemoryDocumentCommentRepository comments = new InMemoryDocumentCommentRepository();

    // Avocat : tout. Client : lire/écrire le partagé, seulement si le document est partagé.
    // Lecteur : lecture seule, sans droit d'écriture sur le dossier.
    private final CommentAccessPolicy policy = (caller, document, shared) -> {
        if (!caller.cabinetId().equals(document.cabinetId())) {
            return Set.of();
        }
        if (caller.userId().equals(clientId)) {
            return shared ? EnumSet.of(CommentRight.READ_SHARED, CommentRight.WRITE_SHARED)
                    : EnumSet.noneOf(CommentRight.class);
        }
        if (caller.userId().equals(readerId)) {
            return EnumSet.of(CommentRight.READ_INTERNAL, CommentRight.READ_SHARED);
        }
        return EnumSet.allOf(CommentRight.class);
    };

    private final AddCommentUseCase add = new AddCommentUseCase(documents, comments, policy);
    private final ListCommentsUseCase list = new ListCommentsUseCase(documents, comments, policy);

    @BeforeEach
    void setUp() {
        documents.register(new Document(documentId, cabinetId, UUID.randomUUID(), "Contrat de bail.pdf",
                DocumentType.PDF, 10, "key-" + documentId, lawyerId, "Karim",
                Instant.parse("2026-11-01T10:00:00Z")));
    }

    private void share() {
        documents.share(cabinetId, documentId, lawyerId, Instant.now());
    }

    @Test
    void l_avocat_ajoute_un_commentaire_interne_et_un_commentaire_partage() {
        DocumentComment internal = add.execute(lawyer, documentId, CommentVisibility.INTERNAL, "  Note interne  ");
        DocumentComment shared = add.execute(lawyer, documentId, CommentVisibility.SHARED, "Question au client");

        assertThat(internal.body()).isEqualTo("Note interne");
        assertThat(internal.authorId()).isEqualTo(lawyerId);
        assertThat(internal.authorName()).isEqualTo("Karim");
        assertThat(internal.cabinetId()).isEqualTo(cabinetId);
        assertThat(shared.visibility()).isEqualTo(CommentVisibility.SHARED);
        assertThat(comments.comments).hasSize(2);
    }

    @Test
    void la_liste_est_triee_du_plus_ancien_au_plus_recent_avec_auteur_et_visibilite() {
        add.execute(lawyer, documentId, CommentVisibility.INTERNAL, "premier");
        add.execute(lawyer, documentId, CommentVisibility.SHARED, "second");

        List<DocumentComment> result = list.execute(lawyer, documentId);

        assertThat(result).extracting(DocumentComment::body).containsExactly("premier", "second");
        assertThat(result).extracting(DocumentComment::visibility)
                .containsExactly(CommentVisibility.INTERNAL, CommentVisibility.SHARED);
        assertThat(result).extracting(DocumentComment::authorName).containsOnly("Karim");
    }

    @Test
    void un_client_ne_voit_que_les_commentaires_partages_et_la_base_ne_lit_pas_les_internes() {
        share();
        add.execute(lawyer, documentId, CommentVisibility.INTERNAL, "stratégie confidentielle");
        add.execute(lawyer, documentId, CommentVisibility.SHARED, "visible du client");

        List<DocumentComment> result = list.execute(client, documentId);

        assertThat(result).extracting(DocumentComment::body).containsExactly("visible du client");
        assertThat(comments.requestedVisibilities.get(comments.requestedVisibilities.size() - 1))
                .containsExactly(CommentVisibility.SHARED);
    }

    @Test
    void un_client_ecrit_un_commentaire_partage_mais_jamais_un_commentaire_interne() {
        share();

        DocumentComment ok = add.execute(client, documentId, CommentVisibility.SHARED, "Voici ma réponse");

        assertThat(ok.authorId()).isEqualTo(clientId);
        assertThatThrownBy(() -> add.execute(client, documentId, CommentVisibility.INTERNAL, "tentative"))
                .isInstanceOf(DocumentAccessDeniedException.class);
        assertThat(comments.comments).hasSize(1);
    }

    @Test
    void un_client_ne_voit_ni_n_ecrit_sur_un_document_non_partage() {
        add.execute(lawyer, documentId, CommentVisibility.SHARED, "préparé pour plus tard");

        assertThatThrownBy(() -> list.execute(client, documentId))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThatThrownBy(() -> add.execute(client, documentId, CommentVisibility.SHARED, "bonjour"))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThat(comments.comments).hasSize(1);
    }

    @Test
    void un_client_perd_l_acces_aux_commentaires_quand_le_partage_est_retire() {
        share();
        add.execute(lawyer, documentId, CommentVisibility.SHARED, "visible");
        documents.revoke(cabinetId, documentId, lawyerId, Instant.now());

        assertThatThrownBy(() -> list.execute(client, documentId))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void un_lecteur_sans_droit_d_ecriture_lit_tout_mais_n_ecrit_rien() {
        add.execute(lawyer, documentId, CommentVisibility.INTERNAL, "interne");

        assertThat(list.execute(reader, documentId)).hasSize(1);
        assertThatThrownBy(() -> add.execute(reader, documentId, CommentVisibility.SHARED, "essai"))
                .isInstanceOf(DocumentAccessDeniedException.class);
        assertThatThrownBy(() -> add.execute(reader, documentId, CommentVisibility.INTERNAL, "essai"))
                .isInstanceOf(DocumentAccessDeniedException.class);
        assertThat(comments.comments).hasSize(1);
    }

    @Test
    void un_autre_cabinet_ne_voit_pas_le_document() {
        add.execute(lawyer, documentId, CommentVisibility.SHARED, "confidentiel");

        assertThatThrownBy(() -> list.execute(foreign, documentId))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThatThrownBy(() -> add.execute(foreign, documentId, CommentVisibility.SHARED, "intrusion"))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThat(comments.comments).hasSize(1);
    }

    @Test
    void refuse_un_commentaire_vide_ou_blanc_avant_toute_ecriture() {
        assertThatThrownBy(() -> add.execute(lawyer, documentId, CommentVisibility.SHARED, "   "))
                .isInstanceOf(InvalidDocumentException.class);
        assertThatThrownBy(() -> add.execute(lawyer, documentId, CommentVisibility.SHARED, null))
                .isInstanceOf(InvalidDocumentException.class);
        assertThat(comments.comments).isEmpty();
    }

    @Test
    void accepte_5000_caracteres_et_refuse_5001() {
        assertThat(add.execute(lawyer, documentId, CommentVisibility.SHARED, "a".repeat(5000)).body())
                .hasSize(5000);
        assertThatThrownBy(() -> add.execute(lawyer, documentId, CommentVisibility.SHARED, "a".repeat(5001)))
                .isInstanceOf(InvalidDocumentException.class);
        assertThat(comments.comments).hasSize(1);
    }

    @Test
    void compte_les_caracteres_et_non_les_unites_utf16() {
        // 5000 émojis = 10000 unités UTF-16, mais 5000 caractères : accepté.
        String body = "\uD83D\uDE00".repeat(5000);

        assertThat(add.execute(lawyer, documentId, CommentVisibility.SHARED, body).body()).isEqualTo(body);
    }
}