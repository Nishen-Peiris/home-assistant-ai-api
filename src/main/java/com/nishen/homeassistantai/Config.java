package com.nishen.homeassistantai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HomeAssistantProperties.class)
public class Config {
}
