package com.shuhuayv.codereviewer.controller;

import com.shuhuayv.codereviewer.common.ApiResponse;
import com.shuhuayv.codereviewer.common.PageResult;
import com.shuhuayv.codereviewer.dto.*;
import com.shuhuayv.codereviewer.service.CodeScanService;
import com.shuhuayv.codereviewer.service.GitRepoService;
import com.shuhuayv.codereviewer.service.RepoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
@Tag(name = "仓库信息管理", description = "代码仓库的增删改查、克隆、扫描接口")
public class RepoController {

    private final RepoService repoService;
    private final GitRepoService gitRepoService;
    private final CodeScanService codeScanService;

    @PostMapping
    @Operation(summary = "创建仓库", description = "添加一个新的代码仓库")
    public ApiResponse<RepoResponse> create(@Valid @RequestBody CreateRepoRequest request) {
        return ApiResponse.success(repoService.create(request));
    }

    @GetMapping
    @Operation(summary = "仓库列表", description = "获取所有仓库信息")
    public ApiResponse<List<RepoResponse>> list() {
        return ApiResponse.success(repoService.listAll());
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询仓库", description = "分页获取仓库列表")
    public ApiResponse<PageResult<RepoResponse>> page(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(repoService.page(pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "仓库详情", description = "根据ID获取仓库信息")
    public ApiResponse<RepoResponse> getById(
            @Parameter(description = "仓库ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(repoService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除仓库", description = "删除指定仓库")
    public ApiResponse<Void> delete(
            @Parameter(description = "仓库ID", example = "1") @PathVariable Long id) {
        repoService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "克隆仓库", description = "使用 JGit 将远程仓库克隆到本地 repos/{id} 目录")
    public ApiResponse<RepoCloneResponse> clone(
            @Parameter(description = "仓库ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(gitRepoService.cloneRepo(id));
    }

    @PostMapping("/{id}/scan")
    @Operation(summary = "扫描代码文件", description = "扫描本地仓库代码文件并写入 code_file 表")
    public ApiResponse<CodeScanResponse> scan(
            @Parameter(description = "仓库ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(codeScanService.scanRepo(id));
    }

    @GetMapping("/{id}/files")
    @Operation(summary = "代码文件列表", description = "查询该仓库的代码文件列表")
    public ApiResponse<List<CodeFileResponse>> listFiles(
            @Parameter(description = "仓库ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(repoService.listCodeFiles(id));
    }

    @GetMapping("/{id}/files/page")
    @Operation(summary = "分页查询代码文件", description = "分页查询代码文件")
    public ApiResponse<PageResult<CodeFileResponse>> pageFiles(
            @Parameter(description = "仓库ID", example = "1") @PathVariable Long id,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(repoService.pageCodeFiles(id, pageNum, pageSize));
    }
}