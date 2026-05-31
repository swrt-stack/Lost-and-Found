package com.example.lostandfound.service;

import com.example.lostandfound.dto.UserDTO;

import java.util.List;

public interface UserService {
    UserDTO.ProfileVO getProfile();

    UserDTO.ProfileVO updateProfile(UserDTO.UpdateProfileRequest request);

    List<UserDTO.UserListVO> listUsers();

    UserDTO.UserActionVO updateStatus(Long id, Integer status);

    UserDTO.UserActionVO updateRole(Long id, String role);
}
