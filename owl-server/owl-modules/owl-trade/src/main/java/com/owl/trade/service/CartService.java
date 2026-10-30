package com.owl.trade.service;

import com.owl.trade.dto.CartAddRequest;
import com.owl.trade.dto.CartCheckRequest;
import com.owl.trade.dto.CartMergeRequest;
import com.owl.trade.dto.CartUpdateRequest;
import com.owl.trade.vo.CartMergeVO;
import com.owl.trade.vo.CartVO;

import java.util.Collection;
import java.util.List;

public interface CartService {

    CartVO get(Long userId);

    CartVO add(Long userId, CartAddRequest request);

    CartVO updateLine(Long userId, String productId, CartUpdateRequest request);

    CartVO remove(Long userId, String productId);

    CartVO check(Long userId, CartCheckRequest request);

    CartVO removeChecked(Long userId);

    void clear(Long userId);

    CartMergeVO merge(Long userId, CartMergeRequest request);

    /** 下单成功后删掉对应行（fromCart=true 时） */
    void removeLines(Long userId, Collection<String> productCodes);

    void deleteAllOfUser(Long userId);

    List<String> codesOf(Long userId);
}
