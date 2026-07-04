package com.orkutclone.api.config;

import com.orkutclone.api.model.User;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String PROFILE_OVERVIEW_CACHE = "profileOverview";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(PROFILE_OVERVIEW_CACHE);
        cacheManager.setCaffeine(com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500));
        return cacheManager;
    }

    @Bean("profileOverviewKeyGenerator")
    public KeyGenerator profileOverviewKeyGenerator() {
        return (target, method, params) -> {
            UUID viewerId = authenticatedUserId();
            UUID targetUserId = params.length > 0 ? (UUID) params[0] : null;
            UUID effectiveTarget = targetUserId == null ? viewerId : targetUserId;
            return viewerId + ":" + effectiveTarget;
        };
    }

    private UUID authenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return Objects.requireNonNull(user.getId(), "Authenticated user id is required");
        }
        throw new IllegalStateException("Authenticated principal is not a User");
    }
}