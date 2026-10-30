package com.owl.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "细分（后台类目维护用）")
public class SubDTO {

    private String code;

    private String name;
}
