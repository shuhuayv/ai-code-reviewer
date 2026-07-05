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
@Schema(description = "评审问题响应")
public class ReviewIssueResponse {

    @Schema(description = "问题ID", example = "1")
    private Long id;

    @Schema(description = "任务ID", example = "1")
    private Long taskId;

    @Schema(description = "文件路径", example = "src/main/java/UserService.java")
    private String filePath;

    @Schema(description = "行号", example = "45")
    private Integer lineNumber;

    @Schema(description = "严重程度", example = "SUGGESTION")
    private String severity;

    @Schema(description = "问题分类", example = "空值检查")
    private String category;

    @Schema(description = "问题标题", example = "建议对返回值进行空值检查")
    private String title;

    @Schema(description = "问题描述", example = "user.getName() 可能返回 null")
    private String description;

    @Schema(description = "修改建议", example = "使用 Optional.ofNullable 包装")
    private String suggestion;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}