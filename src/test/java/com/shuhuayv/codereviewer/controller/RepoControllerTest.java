package com.shuhuayv.codereviewer.controller;

import com.shuhuayv.codereviewer.common.ApiResponse;
import com.shuhuayv.codereviewer.dto.RepoResponse;
import com.shuhuayv.codereviewer.service.CodeScanService;
import com.shuhuayv.codereviewer.service.GitRepoService;
import com.shuhuayv.codereviewer.service.RepoService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RepoController 单元测试：直接实例化（@RequiredArgsConstructor 全参构造），以 Mock Service 验证接口编排。
 * 不加载 Spring MVC，纯逻辑验证。
 */
class RepoControllerTest {

    private final RepoService repoService = mock(RepoService.class);
    private final GitRepoService gitRepoService = mock(GitRepoService.class);
    private final CodeScanService codeScanService = mock(CodeScanService.class);
    private final RepoController controller = new RepoController(repoService, gitRepoService, codeScanService);

    @Test
    void shouldListReposWrappedInApiResponse() {
        RepoResponse r = RepoResponse.builder().id(1L).name("demo").build();
        when(repoService.listAll()).thenReturn(List.of(r));

        ApiResponse<List<RepoResponse>> resp = controller.list();

        assertThat(resp.getCode()).isEqualTo(200);
        assertThat(resp.getData()).hasSize(1);
        assertThat(resp.getData().get(0).getName()).isEqualTo("demo");
    }

    @Test
    void shouldReturnRepoById() {
        RepoResponse r = RepoResponse.builder().id(7L).name("x").build();
        when(repoService.getById(7L)).thenReturn(r);

        ApiResponse<RepoResponse> resp = controller.getById(7L);
        assertThat(resp.getCode()).isEqualTo(200);
        assertThat(resp.getData().getId()).isEqualTo(7L);
    }
}
