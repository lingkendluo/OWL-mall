package com.owl.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员")
public class AdminVO {

    @Schema(description = "字符串 id")
    private String id;

    private String username;

    private String nickname;

    /** super / ops */
    private String role;
}
