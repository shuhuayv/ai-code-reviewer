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
@Schema(description = "代码文件响应")
public class CodeFileResponse {

    @Schema(description = "文件ID", example = "1")
    private Long id;

    @Schema(description = "仓库ID", example = "1")
    private Long repoId;

    @Schema(description = "文件路径", example = "src/main/java/UserService.java")
    private String filePath;

    @Schema(description = "编程语言", example = "Java")
    private String language;

    @Schema(description = "行数", example = "120")
    private Integer lineCount;

    @Schema(description = "内容哈希（SHA-256）", example = "a1b2c3d4...")
    private String contentHash;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}