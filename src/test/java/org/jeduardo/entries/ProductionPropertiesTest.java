package org.jeduardo.entries;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class ProductionPropertiesTest {

    @Test
    void applicationConfigurationsKeepFlywayInControlAndProductionExposesOnlySafeEndpoints() throws Exception {
        Properties productionProperties = loadProperties("src/main/resources/application.properties");
        Properties testProperties = loadProperties("src/test/resources/application.properties");

        assertThat(productionProperties)
                .containsEntry("spring.jpa.hibernate.ddl-auto", "validate")
                .containsEntry("management.endpoints.web.exposure.include", "health,prometheus");
        assertThat(testProperties)
                .containsEntry("spring.jpa.hibernate.ddl-auto", "validate");
    }

    private Properties loadProperties(String path) throws Exception {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of(path))) {
            properties.load(reader);
        }
        return properties;
    }
}
