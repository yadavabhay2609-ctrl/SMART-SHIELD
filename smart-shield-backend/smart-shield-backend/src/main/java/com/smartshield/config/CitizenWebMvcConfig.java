package com.smartshield.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.smartshield.security.CitizenAuthInterceptor;

@Configuration
public class CitizenWebMvcConfig implements WebMvcConfigurer {

    private final CitizenAuthInterceptor citizenAuthInterceptor;

    public CitizenWebMvcConfig(CitizenAuthInterceptor citizenAuthInterceptor) {
        this.citizenAuthInterceptor = citizenAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(citizenAuthInterceptor)
                .addPathPatterns("/api/grievances/**", "/api/safety/**", "/api/ai/**", "/api/forms/**")
                .excludePathPatterns("/api/safety/routes/config", "/api/safety/routes/resolve-location", "/api/safety/routes/destinations", "/api/ai/status", "/api/forms/status");
    }
}
