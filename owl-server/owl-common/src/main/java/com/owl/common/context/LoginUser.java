package com.owl.common.context;

/**
 * 当前登录用户（C 端）。由 JwtAuthInterceptor 解析 token 后放进 UserContext。
 *
 * @param userId 用户主键
 * @param guest  是否游客（游客也要能加购下单，所以不排除在登录态之外）
 * @param jti    当前 access token 的 jti，退出登录时要写黑名单
 */
public record LoginUser(Long userId, boolean guest, String jti) {
}
