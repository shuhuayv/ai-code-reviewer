package com.shuhuayv.codereviewer.controller;

import com.shuhuayv.codereviewer.common.ApiResponse;
import com.shuhuayv.codereviewer.dto.ReviewReportResponse;
import com.shuhuayv.codereviewer.service.ReviewService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ReviewController 单元测试：直接实例化，以 Mock ReviewService 验证接口编排（含 Markdown 纯文本导出）。
 */
class ReviewControllerTest {

    private final ReviewService reviewService = mock(ReviewService.class);
    private final ReviewController controller = new ReviewController(reviewService);

    @Test
    void shouldGetReportMarkdownAsPlainText() {
        when(reviewService.getTaskReportMarkdown(5L)).thenReturn("# Report");

        String md = controller.getTaskReportMarkdown(5L);
        assertThat(md).isEqualTo("# Report");
    }

    @Test
    void shouldGetReportWrappedInApiResponse() {
        ReviewReportResponse report = ReviewReportResponse.builder().id(1L).taskId(5L).summary("ok").build();
        when(reviewService.getTaskReport(5L)).thenReturn(report);

        ApiResponse<ReviewReportResponse> resp = controller.getTaskReport(5L);
        assertThat(resp.getCode()).isEqualTo(200);
        assertThat(resp.getData().getSummary()).isEqualTo("ok");
    }
}
