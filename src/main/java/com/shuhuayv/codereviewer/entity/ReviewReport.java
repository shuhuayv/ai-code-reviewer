package com.shuhuayv.codereviewer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_report")
public class ReviewReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private String summary;
    private String overallAssessment;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}