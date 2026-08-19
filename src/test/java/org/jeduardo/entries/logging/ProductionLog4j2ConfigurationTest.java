package org.jeduardo.entries.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

class ProductionLog4j2ConfigurationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void productionJsonLayoutEmitsRequestIdOnceWithoutAnAdditionalField() throws Exception {
        Path configurationFile = Path.of("src/main/resources/log4j2.xml").toAbsolutePath();
        assertThat(Files.readString(configurationFile)).doesNotContain("EventTemplateAdditionalField");

        LoggerContext context = Configurator.initialize(
                "production-log4j2-test",
                null,
                configurationFile.toUri());
        try {
            Appender appender = context.getConfiguration().getAppender("json");
            Layout<? extends Serializable> layout = appender.getLayout();
            assertThat(layout).isInstanceOf(JsonTemplateLayout.class);

            String requestId = "request-id-under-test";
            LogEvent event = Log4jLogEvent.newBuilder()
                    .setLoggerName(getClass().getName())
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("log event"))
                    .setContextData(ContextDataFactory.createContextData(Map.of("requestId", requestId)))
                    .build();
            JsonNode json = OBJECT_MAPPER.readTree(layout.toSerializable(event).toString());

            assertThat(json.findValues("requestId"))
                    .extracting(JsonNode::asText)
                    .containsExactly(requestId);
        } finally {
            context.stop();
        }
    }
}
