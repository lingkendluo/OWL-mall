package com.owl.trade.vo;

import com.owl.auth.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 个人中心汇总：一次给全，前端不用再发三个请求。
 */
@Data
@Schema(description = "个人中心汇总")
public class ProfileVO {

    private UserVO user;

    private AssetsVO assets;

    private OrderCountVO orderCounts;
}
