package com.owl.common.context;

import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;

/**
 * B 端登录态。
 */
public final class AdminContext {

    private static final ThreadLocal<AdminUser> HOLDER = new ThreadLocal<>();

    private AdminContext() {
    }

    public static void set(AdminUser admin) {
        HOLDER.set(admin);
    }

    public static AdminUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Long requireAdminId() {
        AdminUser admin = HOLDER.get();
        if (admin == null) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return admin.adminId();
    }
}
