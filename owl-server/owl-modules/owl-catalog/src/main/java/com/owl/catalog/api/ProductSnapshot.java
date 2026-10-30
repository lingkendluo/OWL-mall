package com.owl.catalog.api;

/**
 * 跨模块传递的商品快照。trade / payment 只认这个，不碰 Entity。
 * 金额单位是分，转元在各自的 VO 层做。
 */
public record ProductSnapshot(
        Long id,
        String code,
        Long categoryId,
        Long subId,
        String name,
        String spec,
        String image,
        String emoji,
        long priceCent,
        long listCent,
        int stock,
        int status,
        int version,
        String subCode
) {
    public boolean onSale() {
        return status == 1;
    }
}
