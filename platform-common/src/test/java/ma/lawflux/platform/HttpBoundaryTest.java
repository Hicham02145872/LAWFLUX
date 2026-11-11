package ma.lawflux.platform;
import ma.lawflux.platform.config.PlatformConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HttpBoundaryTest.App.class)
@AutoConfigureMockMvc
class HttpBoundaryTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(PlatformConfiguration.class)
    static class App {}
    @Autowired MockMvc mvc;
    @Test void healthIsPublicAndCarriesCorrelation() throws Exception {
        mvc.perform(get("/actuator/health").header("X-Correlation-Id", "test-42"))
            .andExpect(status().isOk()).andExpect(header().string("X-Correlation-Id", "test-42"));
    }
    @Test void businessApiAndEventProbeAreClosedByDefault() throws Exception {
        mvc.perform(get("/api/dossiers")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/system/events")).andExpect(status().isUnauthorized());
    }
}
