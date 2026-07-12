package com.shuhuayv.codereviewer.exception;

import com.shuhuayv.codereviewer.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler 单元测试：直接实例化，验证业务异常 / 未知异常 / 参数校验异常的响应映射。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleBusinessExceptionAsBadRequest() {
        BusinessException ex = new BusinessException(404, "仓库不存在");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusinessException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCode()).isEqualTo(404);
        assertThat(resp.getBody().getMessage()).isEqualTo("仓库不存在");
    }

    @Test
    void shouldHandleGenericExceptionAsInternalError() {
        Exception ex = new RuntimeException("boom");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getCode()).isEqualTo(500);
        assertThat(resp.getBody().getMessage()).contains("boom");
    }

    @Test
    void shouldHandleValidationExceptionAndJoinMessages() {
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "req");
        br.addError(new FieldError("req", "name", "名称不能为空"));
        br.addError(new FieldError("req", "url", "URL 格式错误"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, br);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidationException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).contains("名称不能为空").contains("URL 格式错误");
    }
}
