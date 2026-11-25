package ma.lawflux.documents;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.EnumSet;
import java.util.UUID;
import ma.lawflux.documents.api.grpc.DisabledDocumentServiceEndpoint;
import ma.lawflux.documents.api.grpc.DocumentServiceEndpoint;
import ma.lawflux.documents.application.AddVersionUseCase;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CallerContextProvider;
import ma.lawflux.documents.application.CaseAccessChecker;
import ma.lawflux.documents.application.DocumentAccessPolicy;
import ma.lawflux.documents.application.DocumentContentReader;
import ma.lawflux.documents.application.DocumentStorage;
import ma.lawflux.documents.application.DownloadDocumentUseCase;
import ma.lawflux.documents.application.GetSharingUseCase;
import ma.lawflux.documents.application.ListDocumentsUseCase;
import ma.lawflux.documents.application.ListVersionsUseCase;
import ma.lawflux.documents.application.ShareDocumentUseCase;
import ma.lawflux.documents.application.UnshareDocumentUseCase;
import ma.lawflux.documents.application.UploadDocumentUseCase;
import ma.lawflux.documents.domain.DocumentAction;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.domain.DocumentShareRepository;
import ma.lawflux.documents.domain.DocumentVersionRepository;
import ma.lawflux.documents.support.InMemoryDocumentRepository;
import ma.lawflux.documents.support.InMemoryDocumentShareRepository;
import ma.lawflux.documents.support.InMemoryDocumentStorage;
import ma.lawflux.documents.support.InMemoryDocumentVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ma.lawflux.documents.api.grpc.DocumentSharingEndpoint;

import ma.lawflux.documents.application.AddCommentUseCase;
import ma.lawflux.documents.application.CommentAccessPolicy;
import ma.lawflux.documents.application.CommentRight;
import ma.lawflux.documents.application.ListCommentsUseCase;
import ma.lawflux.documents.domain.DocumentCommentRepository;
import ma.lawflux.documents.support.InMemoryDocumentCommentRepository;

class BusinessWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(ctx -> ctx.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withPropertyValues("lawflux.documents.max-file-size=20MB")
            .withUserConfiguration(
                    DocumentServiceEndpoint.class,
                    DisabledDocumentServiceEndpoint.class,
                    UploadDocumentUseCase.class,
                    ListDocumentsUseCase.class,
                    AddVersionUseCase.class,
                    ListVersionsUseCase.class,
                    GetSharingUseCase.class,
                    ShareDocumentUseCase.class,
                    UnshareDocumentUseCase.class,
                    DownloadDocumentUseCase.class,
                    DocumentSharingEndpoint.class,
                    AddCommentUseCase.class,
                    ListCommentsUseCase.class);

    @Test
    void configuration_par_defaut_demarre_sans_endpoint_metier_ni_beans_manquants() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DisabledDocumentServiceEndpoint.class);
            assertThat(context).doesNotHaveBean(DocumentServiceEndpoint.class);
            assertThat(context).doesNotHaveBean(UploadDocumentUseCase.class);
            assertThat(context).doesNotHaveBean(ListDocumentsUseCase.class);
            assertThat(context).doesNotHaveBean(AddVersionUseCase.class);
            assertThat(context).doesNotHaveBean(ListVersionsUseCase.class);
            assertThat(context).doesNotHaveBean(GetSharingUseCase.class);
            assertThat(context).doesNotHaveBean(ShareDocumentUseCase.class);
            assertThat(context).doesNotHaveBean(UnshareDocumentUseCase.class);
            assertThat(context).doesNotHaveBean(DownloadDocumentUseCase.class);
            assertThat(context).doesNotHaveBean(DocumentSharingEndpoint.class);
            assertThat(context).doesNotHaveBean(AddCommentUseCase.class);
            assertThat(context).doesNotHaveBean(ListCommentsUseCase.class);
        });
    }

    @Test
    void active_l_endpoint_metier_quand_le_flag_et_les_beans_requis_sont_fournis() {
        runner.withPropertyValues("lawflux.documents.business-enabled=true")
                .withBean(DocumentRepository.class, InMemoryDocumentRepository::new)
                .withBean(DocumentVersionRepository.class, InMemoryDocumentVersionRepository::new)
                .withBean(DocumentShareRepository.class, InMemoryDocumentShareRepository::new)
                .withBean(DocumentStorage.class, InMemoryDocumentStorage::new)
                .withBean(CaseAccessChecker.class, () -> (cabinet, caze) -> true)
                .withBean(DocumentAccessPolicy.class,
                        () -> (caller, document, shared) -> EnumSet.of(DocumentAction.DOWNLOAD))
                .withBean(DocumentContentReader.class,
                        () -> storageKey -> new ByteArrayInputStream(new byte[0]))
                .withBean(CallerContextProvider.class,
                        () -> () -> new CallerContext(UUID.randomUUID(), UUID.randomUUID(), "Karim"))
                        .withBean(DocumentCommentRepository.class, InMemoryDocumentCommentRepository::new)
                        .withBean(CommentAccessPolicy.class,
                                () -> (caller, document, shared) -> EnumSet.of(CommentRight.READ_SHARED))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DocumentServiceEndpoint.class);
                    assertThat(context).doesNotHaveBean(DisabledDocumentServiceEndpoint.class);
                    assertThat(context).hasSingleBean(GetSharingUseCase.class);
                    assertThat(context).hasSingleBean(ShareDocumentUseCase.class);
                    assertThat(context).hasSingleBean(UnshareDocumentUseCase.class);
                    assertThat(context).hasSingleBean(DownloadDocumentUseCase.class);
                    assertThat(context).hasSingleBean(DocumentSharingEndpoint.class);
                    assertThat(context).hasSingleBean(AddCommentUseCase.class);
                    assertThat(context).hasSingleBean(ListCommentsUseCase.class);
                });
    }
}