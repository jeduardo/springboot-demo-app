package org.jeduardo.entries;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class ProductionPropertiesTest {

    @Test
    void productionConfigurationKeepsFlywayInControlAndExposesOnlySafeEndpoints() throws Exception {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("src/main/resources/application.properties"))) {
            properties.load(reader);
        }

        assertThat(properties)
                .containsEntry("spring.jpa.hibernate.ddl-auto", "validate")
                .containsEntry("management.endpoints.web.exposure.include", "health,prometheus");
    }
}
