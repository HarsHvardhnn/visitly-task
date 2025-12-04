package com.assignments.rbac.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        
        ConcurrentMapCache userCache = new ConcurrentMapCache("userCache") {
            private final long TTL = 5 * 60 * 1000; // 5 minutes in milliseconds
            private final java.util.concurrent.ConcurrentHashMap<Object, CacheEntry> cacheMap = 
                new java.util.concurrent.ConcurrentHashMap<>();

            @Override
            protected Object lookup(Object key) {
                CacheEntry entry = cacheMap.get(key);
                if (entry != null && !entry.isExpired()) {
                    return entry.getValue();
                } else {
                    cacheMap.remove(key);
                    return null;
                }
            }

            @Override
            public void put(Object key, Object value) {
                cacheMap.put(key, new CacheEntry(value, System.currentTimeMillis() + TTL));
            }

            @Override
            public void evict(Object key) {
                cacheMap.remove(key);
            }

            @Override
            public void clear() {
                cacheMap.clear();
            }

             class CacheEntry {
                private final Object value;
                private final long expirationTime;

                public CacheEntry(Object value, long expirationTime) {
                    this.value = value;
                    this.expirationTime = expirationTime;
                }

                public Object getValue() {
                    return value;
                }

                public boolean isExpired() {
                    return System.currentTimeMillis() > expirationTime;
                }
            }
        };

        cacheManager.setCaches(Arrays.asList(userCache));
        return cacheManager;
    }
}