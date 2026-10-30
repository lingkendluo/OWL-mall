package com.owl.admin.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在后台写操作的方法上，执行成功后自动写 s_admin_log。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminAction {

    /** 动作名，建议用 PRODUCT_CREATE / ORDER_DELIVER 这种大写下划线 */
    String value();
}
