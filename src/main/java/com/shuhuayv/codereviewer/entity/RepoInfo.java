package com.shuhuayv.codereviewer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repo_info")
public class RepoInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String url;
    private String branch;
    private String description;
    private String language;
    private String localPath;
    private String status;
    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}