package com.owl.common.api;

import lombok.Data;

import java.util.List;

/**
 * 分页响应。字段名与文档 6.1 一致：list / total / page / size / pages。
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;
    private int pages;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.list = list == null ? List.of() : list;
        result.total = total;
        result.page = page;
        result.size = size;
        result.pages = size <= 0 ? 0 : (int) ((total + size - 1) / size);
        return result;
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return of(List.of(), 0, page, size);
    }
}
