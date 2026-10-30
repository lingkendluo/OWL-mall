package com.owl.trade.service;

import com.owl.trade.dto.PreviewRequest;
import com.owl.trade.vo.PreviewVO;

public interface CheckoutService {

    /** 服务端重新计价，前端传的价格一律不认（请求体里也没有价格字段） */
    PreviewVO preview(Long userId, PreviewRequest request);
}
