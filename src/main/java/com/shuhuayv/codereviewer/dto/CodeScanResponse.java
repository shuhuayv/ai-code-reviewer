package com.shuhuayv.codereviewer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "代码扫描响应")
public class CodeScanResponse {

    @Schema(description = "仓库ID", example = "1")
    private Long repoId;

    @Schema(description = "已扫描文件数", example = "42")
    private int scannedFileCount;

    @Schema(description = "已跳过文件数", example = "5")
    private int skippedFileCount;

    @Schema(description = "总行数", example = "3850")
    private int totalLineCount;

    @Schema(description = "语言分布统计", example = "{\"Java\": 20, \"Python\": 10}")
    private Map<String, Integer> languages;

    @Schema(description = "耗时（毫秒）", example = "1200")
    private long costMs;
}