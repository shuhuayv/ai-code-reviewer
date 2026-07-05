package com.shuhuayv.codereviewer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuhuayv.codereviewer.entity.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {
}