package com.orkutclone.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @Value("${app.storage.public-base-url:/uploads/avatars}")
    private String publicBaseUrl;

    @Value("${app.storage.album-dir:uploads/albums}")
    private String albumDir;

    @Value("${app.storage.album-public-base-url:/uploads/albums}")
    private String albumPublicBaseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        addResourceHandler(registry, avatarDir, publicBaseUrl);
        addResourceHandler(registry, albumDir, albumPublicBaseUrl);
    }

    private void addResourceHandler(ResourceHandlerRegistry registry, String dir, String publicUrl) {
        String pattern = publicUrl.endsWith("/") ? publicUrl + "**" : publicUrl + "/**";
        String location = Paths.get(dir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> traceMethodFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                if ("TRACE".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
                filterChain.doFilter(request, response);
            }
        });
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
