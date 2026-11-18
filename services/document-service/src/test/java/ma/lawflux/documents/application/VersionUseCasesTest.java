package ma.lawflux.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentVersion;
import ma.lawflux.documents.domain.InvalidDocumentException;
import ma.lawflux.documents.support.InMemoryDocumentStorage;
import ma.lawflux.documents.support.InMemoryDocumentVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class VersionUseCasesTest {

    private final UUID cabinetId = UUID.randomUUID();
    private final UUID otherCabinetId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    private final InMemoryDocumentVersionRepository versions = new InMemoryDocumentVersionRepository();
    private final InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
    private final AddVersionUseCase add =
            new AddVersionUseCase(versions, storage, DataSize.ofMegabytes(20));
    private final ListVersionsUseCase list = new ListVersionsUseCase(versions);

    @BeforeEach
    void setUp() {
        versions.registerDocument(cabinetId, documentId);
    }

    private AddVersionCommand command(UUID cabinet, String fileName, long size) {
        return new AddVersionCommand(cabinet, documentId, UUID.randomUUID(), "Sara",
                fileName, size, new ByteArrayInputStream(new byte[10]));
    }

    @Test
    void ajoute_des_versions_avec_les_numeros_suivants() {
        assertThat(add.execute(command(cabinetId, "v2.pdf", 1024)).number()).isEqualTo(2);
        assertThat(add.execute(command(cabinetId, "v3.docx", 2048)).number()).isEqualTo(3);
        assertThat(storage.files).hasSize(2);
    }

    @Test
    void refuse_un_document_d_un_autre_cabinet_sans_rien_stocker() {
        assertThatThrownBy(() -> add.execute(command(otherCabinetId, "v2.pdf", 1024)))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThat(storage.files).isEmpty();
    }

    @Test
    void refuse_un_type_non_pris_en_charge_sans_rien_stocker() {
        assertThatThrownBy(() -> add.execute(command(cabinetId, "notes.txt", 1024)))
                .isInstanceOf(InvalidDocumentException.class);
        assertThat(storage.files).isEmpty();
    }

    @Test
    void refuse_un_fichier_trop_volumineux() {
        long tooBig = DataSize.ofMegabytes(21).toBytes();
        assertThatThrownBy(() -> add.execute(command(cabinetId, "gros.pdf", tooBig)))
                .isInstanceOf(InvalidDocumentException.class);
        assertThat(storage.files).isEmpty();
    }

    @Test
    void supprime_uniquement_le_fichier_qui_vient_d_etre_stocke_si_la_base_echoue() {
        DocumentVersion kept = add.execute(command(cabinetId, "v2.pdf", 1024));

        versions.failAdd = true;
        assertThatThrownBy(() -> add.execute(command(cabinetId, "v3.pdf", 1024)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(storage.deleted).hasSize(1).doesNotContain(kept.storageKey());
        assertThat(storage.files).containsKey(kept.storageKey());
    }

    @Test
    void liste_du_plus_recent_au_plus_ancien() {
        add.execute(command(cabinetId, "v2.pdf", 1024));

        assertThat(list.execute(cabinetId, documentId))
                .extracting(DocumentVersion::number)
                .containsExactly(2, 1);
    }

    @Test
    void refuse_de_lister_les_versions_d_un_document_d_un_autre_cabinet() {
        assertThatThrownBy(() -> list.execute(otherCabinetId, documentId))
                .isInstanceOf(DocumentNotFoundException.class);
    }
}