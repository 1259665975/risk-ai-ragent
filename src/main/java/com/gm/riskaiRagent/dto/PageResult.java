package com.gm.riskaiRagent.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> records;

    

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setRecords(records);
        return result;
    }
}
