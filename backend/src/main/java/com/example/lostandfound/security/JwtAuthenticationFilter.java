package com.example.lostandfound.security;

import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.UserMapper;
import com.example.lostandfound.util.JwtUtil;
import com.mybatisflex.core.query.QueryWrapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final TokenSessionService tokenSessionService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserMapper userMapper, TokenSessionService tokenSessionService) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                String token = authorization.substring(7);
                if (tokenSessionService.isBlacklisted(token)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                Claims claims = jwtUtil.parseToken(token);
                String username = claims.getSubject();
                long issuedAtMillis = claims.getIssuedAt() == null ? 0L : claims.getIssuedAt().getTime();
                if (tokenSessionService.isUserSessionInvalid(username, issuedAtMillis)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userMapper.selectOneByQuery(QueryWrapper.create().where("username = ?", username));
                    if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    String role = user.getRole() == null ? "USER" : user.getRole();
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
