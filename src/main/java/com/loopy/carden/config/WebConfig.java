package com.loopy.carden.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.security")
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    // Remove CORS configuration to avoid conflicts with SecurityConfig
    // CORS is now handled entirely in SecurityConfig
    
    // Keep other WebMvc configurations if needed in the future
}

