package com.owl.common.context;

/**
 * 当前管理员（B 端）。与 C 端两套 token、两套 ThreadLocal，互不通用。
 */
public record AdminUser(Long adminId, String username, String role, String jti) {
}
