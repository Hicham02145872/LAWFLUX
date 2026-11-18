package ma.lawflux.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.CaseAccessDeniedException;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class UploadDocumentUseCaseTest {

    private final UUID cabinetId = UUID.randomUUID();
    private final UUID caseId = UUID.randomUUID();
    private final UUID foreignCaseId = UUID.randomUUID();

    private final List<Document> saved = new ArrayList<>();
    private final List<String> stored = new ArrayList<>();
    private final List<String> deleted = new ArrayList<>();
    private boolean failSave = false;

    private final DocumentRepository repository = new DocumentRepository() {
        @Override
        public Document save(Document d) {
            if (failSave) throw new IllegalStateException("db indisponible");
            saved.add(d);
            return d;
        }

        @Override
        public List<Document> findByCase(UUID cabinetId, UUID caseId) {
            return List.of();
        }
    };

    private final DocumentStorage storage = new DocumentStorage() {
        @Override
        public String store(UUID cabinetId, String name, InputStream content, long size) {
            String key = "key-" + UUID.randomUUID();
            stored.add(key);
            return key;
        }

        @Override
        public void delete(String storageKey) {
            deleted.add(storageKey);
        }
    };

    // Seul le dossier du cabinet appelant est accessible.
    private final CaseAccessChecker caseAccess =
            (cabinet, caze) -> cabinet.equals(cabinetId) && caze.equals(caseId);

    private final UploadDocumentUseCase useCase =
            new UploadDocumentUseCase(repository, storage, caseAccess, DataSize.ofMegabytes(20));

    private UploadCommand command(UUID caze, String fileName, long size) {
        return new UploadCommand(cabinetId, caze, UUID.randomUUID(),
                "Karim", fileName, size, new ByteArrayInputStream(new byte[10]));
    }

    private UploadCommand command(String fileName, long size) {
        return command(caseId, fileName, size);
    }

    @Test
    void accepte_un_pdf_pour_un_dossier_du_cabinet_appelant() {
        Document d = useCase.execute(command("Contrat de bail.pdf", 1024));
        assertThat(d.type()).isEqualTo(DocumentType.PDF);
        assertThat(d.caseId()).isEqualTo(caseId);
        assertThat(saved).containsExactly(d);
        assertThat(stored).hasSize(1);
    }

    @Test
    void refuse_un_dossier_d_un_autre_cabinet_sans_rien_stocker() {
        assertThatThrownBy(() -> useCase.execute(command(foreignCaseId, "contrat.pdf", 1024)))
                .isInstanceOf(CaseAccessDeniedException.class);
        assertThat(stored).isEmpty();
        assertThat(saved).isEmpty();
    }

    @Test
    void refuse_un_type_non_pris_en_charge_sans_rien_stocker() {
        assertThatThrownBy(() -> useCase.execute(command("notes.txt", 1024)))
                .isInstanceOf(InvalidDocumentException.class);
        assertThat(stored).isEmpty();
        assertThat(saved).isEmpty();
    }

    @Test
    void refuse_un_fichier_trop_volumineux() {
        long tooBig = DataSize.ofMegabytes(21).toBytes();
        assertThatThrownBy(() -> useCase.execute(command("gros.pdf", tooBig)))
                .isInstanceOf(InvalidDocumentException.class);
        assertThat(stored).isEmpty();
    }

    @Test
    void refuse_un_fichier_vide() {
        assertThatThrownBy(() -> useCase.execute(command("vide.pdf", 0)))
                .isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void refuse_un_nom_avec_chemin() {
        assertThatThrownBy(() -> useCase.execute(command("../secret.pdf", 1024)))
                .isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void supprime_le_fichier_stocke_si_la_base_echoue() {
        failSave = true;
        assertThatThrownBy(() -> useCase.execute(command("contrat.docx", 1024)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(deleted).isEqualTo(stored);
    }

    @Test
    void accepte_un_nom_de_255_caracteres() {
        String name = "a".repeat(251) + ".pdf";
        assertThat(name).hasSize(255);
        assertThat(useCase.execute(command(name, 1024)).name()).isEqualTo(name);
    }

    @Test
    void refuse_un_nom_de_256_caracteres_avant_tout_stockage() {
        String name = "a".repeat(252) + ".pdf";
        assertThatThrownBy(() -> useCase.execute(command(name, 1024)))
                .isInstanceOf(InvalidDocumentException.class);
        assertThat(stored).isEmpty();
    }

    @Test
    void compte_les_caracteres_et_non_les_unites_utf16() {
        // 251 émojis + ".pdf" = 255 caractères, mais 506 unités UTF-16.
        String name = "\uD83D\uDE00".repeat(251) + ".pdf";
        assertThat(useCase.execute(command(name, 1024)).name()).isEqualTo(name);
    }
}