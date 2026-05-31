package com.example.lostandfound.config;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.security.JwtAuthenticationFilter;
import com.example.lostandfound.security.RequestIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final RequestIdFilter requestIdFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(RequestIdFilter requestIdFilter,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper) {
        this.requestIdFilter = requestIdFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ApiResponse.fail(401, "Unauthorized")))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN, ApiResponse.fail(403, "Forbidden")))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(EndpointRequest.to("health", "info", "metrics", "prometheus")).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/captcha").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/health/**", "/uploads/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/items/search", "/api/system/dict", "/api/system/overview", "/api/system/announcements", "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/items/lost", "/api/items/found", "/api/upload/**").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/user/profile", "/api/messages", "/api/chats/**", "/api/items/mine", "/api/items/claims", "/api/items/*/chats/**").authenticated()
                        .requestMatchers("/api/ai/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/user/profile").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/messages/*/read", "/api/items/*/chats/messages").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/items/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/items/*/claim", "/api/items/*/complete", "/api/items/*/report", "/api/items/claims/*/approve", "/api/items/claims/*/reject", "/api/items/*/offline").authenticated()
                        .requestMatchers("/api/admin/**").hasAnyRole("REVIEW_ADMIN", "SYS_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/user/list", "/api/logs", "/api/config/system").hasRole("SYS_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/user/*/status", "/api/user/*/role").hasRole("SYS_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasRole("SYS_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/*").hasRole("SYS_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/config/system").hasRole("SYS_ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeJson(HttpServletResponse response, int status, ApiResponse<Void> body) {
        try {
            response.setStatus(status);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (Exception ignored) {
            response.setStatus(status);
        }
    }
}
