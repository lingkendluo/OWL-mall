package com.owl.catalog.mapper.row;

import lombok.Data;

@Data
public class SubCountRow {

    private Long categoryId;
    private Long id;
    private String code;
    private String name;
    private Integer cnt;
}
