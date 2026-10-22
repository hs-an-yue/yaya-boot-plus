package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.entity.SysFile;
import com.yaya.system.domain.query.SysFileQuery;
import com.yaya.system.service.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/system/file")
public class SysFileController {

    @Resource
    private SysFileService sysFileService;


    @Operation(summary = "分页查询文件")
    @PostMapping("/getFilePage")
    public Result<PageDTO<SysFile>> getFilePage(@RequestBody @Valid SysFileQuery query) {
        return Result.ok(sysFileService.getFilePage(query));
    }

    @Operation(summary = "上传文件")
    @LogCollect(module = "文件管理-上传", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping(value = "/uploadFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<SysFile> uploadFile(@RequestParam("file") MultipartFile file) {
        return Result.ok(sysFileService.uploadFile(file));
    }

    @Operation(summary = "下载文件(需登录)")
    @Parameters(value = {
        @Parameter(name = "fileId", description = "文件ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "文件流",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
    @PostMapping("/downloadFile")
    public ResponseEntity<FileSystemResource> downloadFile(@RequestParam("fileId") Long fileId) throws IOException {
        SysFile file = sysFileService.getFileDetail(fileId);
        FileSystemResource resource = sysFileService.downloadFile(fileId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @Operation(summary = "删除文件")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('file:del')")
    @Parameters(value = {
        @Parameter(name = "fileId", description = "文件ID", required = true)
    })
    @LogCollect(module = "文件管理-删除", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/deleteFile")
    public Result<Void> deleteFile(@RequestParam("fileId") Long fileId) {
        return sysFileService.deleteFile(fileId) ? Result.ok() : Result.error("删除文件失败");
    }
}
