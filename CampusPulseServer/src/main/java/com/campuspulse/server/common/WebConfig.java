package com.campuspulse.server.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.campuspulse.server.admin.AdminAuthInterceptor;
import com.campuspulse.server.admin.AdminCsrfInterceptor;
import com.campuspulse.server.auth.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final AdminCsrfInterceptor adminCsrfInterceptor;

    public WebConfig(AuthInterceptor authInterceptor,
                     AdminAuthInterceptor adminAuthInterceptor,
                     AdminCsrfInterceptor adminCsrfInterceptor) {
        this.authInterceptor = authInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.adminCsrfInterceptor = adminCsrfInterceptor;
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

        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin", "/admin/**")
                .excludePathPatterns("/admin/login");

        registry.addInterceptor(adminCsrfInterceptor)
                .addPathPatterns("/admin", "/admin/**")
                .excludePathPatterns("/admin/login");
    }
}
