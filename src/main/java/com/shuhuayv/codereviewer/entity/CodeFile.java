package com.shuhuayv.codereviewer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("code_file")
public class CodeFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long repoId;
    private String filePath;
    private String fileName;
    private String language;
    private String content;
    private Integer charCount;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}