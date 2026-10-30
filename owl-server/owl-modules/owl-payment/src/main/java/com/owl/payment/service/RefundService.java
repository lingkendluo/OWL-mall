package com.owl.payment.service;

import com.owl.payment.dto.RefundRequest;
import com.owl.payment.vo.RefundVO;

public interface RefundService {

    RefundVO apply(Long userId, RefundRequest request);

    RefundVO detail(String refundNo, Long userId);
}
