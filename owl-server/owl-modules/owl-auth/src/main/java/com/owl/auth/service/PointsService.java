package com.owl.auth.service;

import com.owl.auth.vo.PointsLogVO;
import com.owl.common.api.PageResult;

public interface PointsService {

    PageResult<PointsLogVO> list(Long userId, int page, int size);

    /** 积分变动同时写流水与用户余额，type 见 c 文档 5.3 注释 */
    void add(Long userId, int delta, int type, String refNo, String remark);
}
