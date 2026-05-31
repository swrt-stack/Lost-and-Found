package com.example.lostandfound.config;

import com.example.lostandfound.ratelimit.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String[] locations = resolveUploadLocations();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(locations);
    }

    private String[] resolveUploadLocations() {
        Set<String> locations = new LinkedHashSet<>();
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

        addLocation(locations, Paths.get(uploadDir));
        addLocation(locations, userDir.resolve("uploads"));
        addLocation(locations, userDir.resolve("backend").resolve("uploads"));
        Path parent = userDir.getParent();
        if (parent != null) {
            addLocation(locations, parent.resolve("uploads"));
            addLocation(locations, parent.resolve("backend").resolve("uploads"));
        }

        return locations.toArray(String[]::new);
    }

    private void addLocation(Set<String> locations, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        locations.add("file:" + normalized + "/");
    }
}
