package com.shuhuayv.codereviewer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Code Reviewer API")
                        .version("1.0.0")
                        .description("基于大模型的代码评审平台 REST API 文档")
                        .contact(new Contact()
                                .name("Dev Team")
                                .email("dev@shuhuayv.com")));
    }
}