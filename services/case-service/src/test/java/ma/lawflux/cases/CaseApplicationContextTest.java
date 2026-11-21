package ma.lawflux.cases;

import static org.junit.jupiter.api.Assertions.*;

import ma.lawflux.cases.api.grpc.CaseServiceEndpoint;
import ma.lawflux.cases.application.CaseAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"server.port=0", "spring.grpc.server.port=0"})
class CaseApplicationContextTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CaseAccessService access;
    @Autowired CaseServiceEndpoint endpoint;
    @Autowired JdbcTemplate jdbc;

    @Test
    void startsWithJwtBoundaryAndMigratedAccessTables() {
        assertNotNull(access);
        assertNotNull(endpoint);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM case_records", Integer.class));
    }
}
