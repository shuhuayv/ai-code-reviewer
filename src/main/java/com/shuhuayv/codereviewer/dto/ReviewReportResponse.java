package com.shuhuayv.codereviewer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "评审报告响应")
public class ReviewReportResponse {

    @Schema(description = "报告ID", example = "1")
    private Long id;

    @Schema(description = "任务ID", example = "1")
    private Long taskId;

    @Schema(description = "评审摘要")
    private String summary;

    @Schema(description = "总体评价")
    private String overallAssessment;

    @Schema(description = "Markdown 报告")
    private String markdownContent;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}