package org.jeduardo.entries;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableSpringBootMetricsCollector
@EnablePrometheusEndpoint
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}