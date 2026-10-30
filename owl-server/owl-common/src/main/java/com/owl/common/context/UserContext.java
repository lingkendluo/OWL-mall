package com.owl.common.context;

import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;

/**
 * C 端登录态。拦截器 set，afterCompletion 必须 clear —— 线程池复用不清会串号。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** Controller 取当前用户统一走这里，禁止从请求参数里拿 userId */
    public static Long requireUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user.userId();
    }

    public static boolean isGuest() {
        LoginUser user = HOLDER.get();
        return user != null && user.guest();
    }

    public static Long userIdOrNull() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }
}
