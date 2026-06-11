package com.nishen.homeassistantai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class HomeAssistantClient {
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {};
    private static final String AREAS_TEMPLATE = """
            {% set ns = namespace(items=[]) %}
            {% for area in areas() %}
              {% set ns.items = ns.items + [dict(
                area_id=area,
                name=area_name(area),
                entities=area_entities(area),
                devices=area_devices(area)
              )] %}
            {% endfor %}
            {{ ns.items | to_json }}
            """;
    private static final String DEVICES_TEMPLATE = """
            {% set ns = namespace(items=[]) %}
            {% for device in devices() %}
              {% set ns.items = ns.items + [dict(
                device_id=device,
                name=device_name(device),
                area_id=area_id(device),
                area_name=area_name(device),
                manufacturer=device_attr(device, 'manufacturer'),
                model=device_attr(device, 'model'),
                entities=device_entities(device)
              )] %}
            {% endfor %}
            {{ ns.items | to_json }}
            """;

    private final WebClient client;
    private final ObjectMapper objectMapper;

    public HomeAssistantClient(ObjectMapper objectMapper, HomeAssistantProperties properties) {
        this.objectMapper = objectMapper;
        this.client = WebClient.builder()
                .baseUrl(properties.url())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> states() {
        return client.get()
                .uri("/api/states")
                .retrieve()
                .bodyToFlux(Map.class)
                .map(item -> (Map<String, Object>) item)
                .collectList();
    }

    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> state(String entityId) {
        return client.get()
                .uri("/api/states/{entityId}", entityId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(item -> (Map<String, Object>) item);
    }

    public Mono<List<Map<String, Object>>> areas() {
        return renderTemplateList(AREAS_TEMPLATE);
    }

    public Mono<List<Map<String, Object>>> devices() {
        return renderTemplateList(DEVICES_TEMPLATE);
    }

    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> services() {
        return client.get()
                .uri("/api/services")
                .retrieve()
                .bodyToFlux(Map.class)
                .map(item -> (Map<String, Object>) item)
                .collectList();
    }

    private Mono<List<Map<String, Object>>> renderTemplateList(String template) {
        return client.post()
                .uri("/api/template")
                .bodyValue(Map.of("template", template))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseListOfMaps);
    }

    private List<Map<String, Object>> parseListOfMaps(String json) {
        try {
            return objectMapper.readValue(json, LIST_OF_MAPS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Home Assistant template response: " + json, e);
        }
    }
}
