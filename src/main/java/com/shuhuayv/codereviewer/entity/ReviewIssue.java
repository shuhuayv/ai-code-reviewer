package com.shuhuayv.codereviewer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_issue")
public class ReviewIssue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private String filePath;
    private Integer lineNumber;
    private String severity;
    private String category;
    private String title;
    private String description;
    private String suggestion;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}