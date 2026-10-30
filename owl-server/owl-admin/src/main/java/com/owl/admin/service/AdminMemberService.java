package com.owl.admin.service;

import com.owl.admin.vo.AdminMemberVO;
import com.owl.common.api.PageResult;

public interface AdminMemberService {

    /**
     * @param keyword 昵称 / 手机号 / 会员号模糊搜索
     * @param status  1 正常 0 禁用，null 全部
     */
    PageResult<AdminMemberVO> list(String keyword, Integer status, int page, int size);

    /** 启用/禁用会员，1 正常 0 禁用 */
    AdminMemberVO changeStatus(String id, Integer status);
}
