package demo.service;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

@SpringBootApplication
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    Jackson2ObjectMapperBuilder Jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
            .indentOutput(true)
            .modules(new Jdk8Module(), new JavaTimeModule())
            .featuresToDisable(WRITE_DATES_AS_TIMESTAMPS, ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }

}
