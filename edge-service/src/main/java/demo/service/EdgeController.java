package demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;

@RestController
public class EdgeController {

    private final WebClient webClient;

    public EdgeController(
        final @Value("${innerservice.url}") String innerServiceUrl,
        final WebClient.Builder webClientBuilder) {

        this.webClient = webClientBuilder.baseUrl(innerServiceUrl).build();
    }

    @GetMapping(path = "/data1")
    public Mono<Response> data1() {

        return webClient.get()
            .uri("/retrieveData")
            .retrieve()
            .bodyToMono(Response.class);

    }

    @GetMapping(path = "/data2")
    public Mono<Response> data2() {

        return webClient.get()
            .uri("/retrieveData")
            .retrieve()
            .bodyToMono(Response.class)

            // error handling
            .onErrorMap(
                WebClientResponseException.class,
                e -> new RuntimeException(e.getResponseBodyAsString(), e))

            .onErrorMap(
                UnknownHostException.class,
                e -> new RuntimeException("internal service address unknown: " + e.getMessage(), e))

            .onErrorMap(
                ConnectException.class,
                e -> new RuntimeException("internal service not available: data service", e));

    }
}
