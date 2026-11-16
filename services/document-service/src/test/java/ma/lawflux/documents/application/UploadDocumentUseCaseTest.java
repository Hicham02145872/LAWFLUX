package ma.lawflux.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class UploadDocumentUseCaseTest {

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

    private final UploadDocumentUseCase useCase =
            new UploadDocumentUseCase(repository, storage, DataSize.ofMegabytes(20));

    private UploadCommand command(String fileName, long size) {
        return new UploadCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Karim", fileName, size, new ByteArrayInputStream(new byte[10]));
    }

    @Test
    void importe_un_pdf_valide() {
        Document d = useCase.execute(command("Contrat de bail.pdf", 1024));
        assertThat(d.type()).isEqualTo(DocumentType.PDF);
        assertThat(saved).containsExactly(d);
        assertThat(stored).hasSize(1);
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
}