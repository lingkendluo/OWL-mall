package com.owl.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "未读数")
public class UnreadVO {

    private Integer total;
    private Integer order;
    private Integer promo;
    private Integer system;
}
