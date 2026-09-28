package com.docqa.common.api;

import lombok.Data;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页结果
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        PageResult<T> p = new PageResult<>();
        p.records = records;
        p.total = total;
        p.page = page;
        p.size = size;
        return p;
    }

    public <V> PageResult<V> map(Function<T, V> mapper) {
        PageResult<V> p = new PageResult<>();
        p.records = this.records.stream().map(mapper).collect(Collectors.toList());
        p.total = this.total;
        p.page = this.page;
        p.size = this.size;
        return p;
    }
}
