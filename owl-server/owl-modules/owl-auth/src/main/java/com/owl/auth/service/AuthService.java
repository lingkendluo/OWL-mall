package com.owl.auth.service;

import com.owl.auth.dto.ChangePasswordRequest;
import com.owl.auth.dto.LoginRequest;
import com.owl.auth.dto.RegisterRequest;
import com.owl.auth.dto.UpdateProfileRequest;
import com.owl.auth.vo.LoginVO;
import com.owl.auth.vo.TokenVO;
import com.owl.auth.vo.UserVO;

public interface AuthService {

    LoginVO register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    LoginVO guest();

    TokenVO refresh(String refreshToken);

    void logout();

    UserVO me(Long userId);

    UserVO updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
