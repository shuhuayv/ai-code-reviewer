package com.shuhuayv.codereviewer.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "总记录数", example = "100")
    private long total;

    @Schema(description = "当前页码", example = "1")
    private long pageNum;

    @Schema(description = "每页大小", example = "10")
    private long pageSize;

    @Schema(description = "数据列表")
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