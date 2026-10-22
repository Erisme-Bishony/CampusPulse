package com.campuspulse.server.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.campuspulse.server.auth.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/api/v1/users/**",
                        "/api/v1/activities/*/registrations",
                        "/api/v1/activities/*/registration-status",
                        "/api/v1/behaviors/**",
                        "/api/v1/recommendations/**",
                        "/api/v1/reminders/**");
    }
}
