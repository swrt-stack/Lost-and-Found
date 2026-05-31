package com.example.lostandfound.security;

import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.UserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final UserMapper userMapper;

    public CurrentUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public String requireUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(401, "Please login first");
        }
        return authentication.getName();
    }

    public String currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
    }

    public User requireUser() {
        User user = userMapper.selectOneByQuery(QueryWrapper.create().where("username = ?", requireUsername()));
        if (user == null) {
            throw new BusinessException(401, "Current user does not exist");
        }
        return user;
    }
}
