package com.owl.admin.service;

import com.owl.admin.vo.AdminLogVO;
import com.owl.common.api.PageResult;

public interface AdminLogService {

    /**
     * 写后台操作日志。由 AOP 统一调用，业务代码不用关心。
     *
     * @param action PRODUCT_UPDATE / ORDER_DELIVER 之类
     * @param target 操作对象，商品 code 或订单号
     * @param detail 请求内容摘要
     * @param ip     操作来源 IP
     */
    void record(String action, String target, String detail, String ip);

    /**
     * 查日志。keyword 同时匹配 target 与 detail，action 精确匹配。
     */
    PageResult<AdminLogVO> page(String keyword, String action, int page, int size);
}
