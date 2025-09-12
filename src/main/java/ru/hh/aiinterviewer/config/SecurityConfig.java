package ru.hh.aiinterviewer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AuthProperties.class, ApplicationProperties.class})
public class SecurityConfig {

  // Security config is defined in existing SecurityConfig in project; leaving properties enablement only here.
}


