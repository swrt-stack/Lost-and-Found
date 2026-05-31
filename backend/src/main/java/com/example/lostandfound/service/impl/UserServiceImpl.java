package com.example.lostandfound.service.impl;

import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.dto.UserDTO;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.UserMapper;
import com.example.lostandfound.security.CurrentUserService;
import com.example.lostandfound.security.TokenSessionService;
import com.example.lostandfound.service.UserService;
import com.example.lostandfound.service.support.AuditLogSupport;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "REVIEW_ADMIN", "SYS_ADMIN");

    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogSupport auditLogSupport;
    private final TokenSessionService tokenSessionService;

    public UserServiceImpl(UserMapper userMapper, CurrentUserService currentUserService,
                           AuditLogSupport auditLogSupport, TokenSessionService tokenSessionService) {
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
        this.auditLogSupport = auditLogSupport;
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    public UserDTO.ProfileVO getProfile() {
        User user = currentUserService.requireUser();
        return toProfile(user);
    }

    @Override
    public UserDTO.ProfileVO updateProfile(UserDTO.UpdateProfileRequest request) {
        User user = currentUserService.requireUser();
        user.setNickname(request.getNickname().trim());
        user.setPhone(request.getPhone().trim());
        user.setAvatarUrl(blankToNull(request.getAvatarUrl()));
        userMapper.update(user);
        auditLogSupport.record(user.getId(), "UPDATE_PROFILE", "Updated personal profile");
        return toProfile(user);
    }

    @Override
    public List<UserDTO.UserListVO> listUsers() {
        return userMapper.selectAll().stream()
                .map(user -> new UserDTO.UserListVO(
                        user.getId(),
                        user.getUsername(),
                        user.getNickname(),
                        user.getPhone(),
                        user.getAvatarUrl(),
                        user.getRole(),
                        user.getStatus() != null && user.getStatus() == 1 ? "ACTIVE" : "DISABLED"
                ))
                .toList();
    }

    @Override
    public UserDTO.UserActionVO updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "Invalid status");
        }
        User current = currentUserService.requireUser();
        User user = requireUserById(id);
        if (current.getId().equals(user.getId()) && status == 0) {
            throw new BusinessException(400, "You cannot disable your own account");
        }
        user.setStatus(status);
        userMapper.update(user);
        if (status == 0) {
            tokenSessionService.invalidateUserSessions(user.getUsername());
        }
        auditLogSupport.record(current.getId(), "UPDATE_USER_STATUS", "Updated user status: " + user.getUsername());
        return new UserDTO.UserActionVO(user.getId(), status == 1 ? "User enabled" : "User disabled");
    }

    @Override
    public UserDTO.UserActionVO updateRole(Long id, String role) {
        if (role == null || !ALLOWED_ROLES.contains(role)) {
            throw new BusinessException(400, "Invalid role");
        }
        User current = currentUserService.requireUser();
        User user = requireUserById(id);
        if (current.getId().equals(user.getId()) && !"SYS_ADMIN".equals(role)) {
            throw new BusinessException(400, "You cannot downgrade your own account");
        }
        user.setRole(role);
        userMapper.update(user);
        tokenSessionService.invalidateUserSessions(user.getUsername());
        auditLogSupport.record(current.getId(), "UPDATE_USER_ROLE", "Updated user role: " + user.getUsername());
        return new UserDTO.UserActionVO(user.getId(), "Role updated");
    }

    private User requireUserById(Long id) {
        User user = userMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", id));
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return user;
    }

    private UserDTO.ProfileVO toProfile(User user) {
        return new UserDTO.ProfileVO(user.getUsername(), user.getNickname(), user.getPhone(), user.getAvatarUrl(), user.getRole());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
