package ma.lawflux.documents;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import ma.lawflux.documents.api.grpc.DisabledDocumentServiceEndpoint;
import ma.lawflux.documents.api.grpc.DocumentServiceEndpoint;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CallerContextProvider;
import ma.lawflux.documents.application.CaseAccessChecker;
import ma.lawflux.documents.application.DocumentStorage;
import ma.lawflux.documents.application.ListDocumentsUseCase;
import ma.lawflux.documents.application.UploadDocumentUseCase;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.support.InMemoryDocumentRepository;
import ma.lawflux.documents.support.InMemoryDocumentStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BusinessWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(ctx -> ctx.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withPropertyValues("lawflux.documents.max-file-size=20MB")
            .withUserConfiguration(
                    DocumentServiceEndpoint.class,
                    DisabledDocumentServiceEndpoint.class,
                    UploadDocumentUseCase.class,
                    ListDocumentsUseCase.class);

    @Test
    void configuration_par_defaut_demarre_sans_endpoint_metier_ni_beans_manquants() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DisabledDocumentServiceEndpoint.class);
            assertThat(context).doesNotHaveBean(DocumentServiceEndpoint.class);
            assertThat(context).doesNotHaveBean(UploadDocumentUseCase.class);
            assertThat(context).doesNotHaveBean(ListDocumentsUseCase.class);
        });
    }

    @Test
    void active_l_endpoint_metier_quand_le_flag_et_les_beans_requis_sont_fournis() {
        runner.withPropertyValues("lawflux.documents.business-enabled=true")
                .withBean(DocumentRepository.class, InMemoryDocumentRepository::new)
                .withBean(DocumentStorage.class, InMemoryDocumentStorage::new)
                .withBean(CaseAccessChecker.class, () -> (cabinet, caze) -> true)
                .withBean(CallerContextProvider.class,
                        () -> () -> new CallerContext(UUID.randomUUID(), UUID.randomUUID(), "Karim"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DocumentServiceEndpoint.class);
                    assertThat(context).doesNotHaveBean(DisabledDocumentServiceEndpoint.class);
                });
    }
}