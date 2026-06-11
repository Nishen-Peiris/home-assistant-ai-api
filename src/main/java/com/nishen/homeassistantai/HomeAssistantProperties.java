package com.nishen.homeassistantai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "home-assistant")
public record HomeAssistantProperties(String url, String token) {
}
