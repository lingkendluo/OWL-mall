package com.owl.admin.service;

import com.owl.admin.dto.AdminLoginRequest;
import com.owl.admin.vo.AdminLoginVO;
import com.owl.admin.vo.AdminVO;

public interface AdminAuthService {

    AdminLoginVO login(AdminLoginRequest request);

    /** 当前登录管理员，用于刷新页面后确认身份 */
    AdminVO me();
}
