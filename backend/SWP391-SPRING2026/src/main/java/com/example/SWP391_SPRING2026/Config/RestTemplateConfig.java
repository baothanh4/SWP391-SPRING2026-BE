package com.example.SWP391_SPRING2026.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {

            System.out.println("=== REQUEST HEADERS ===");
            request.getHeaders().forEach((k, v) ->
                    System.out.println(k + ":" + v));

            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
