package com.ecommerce.file.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.file.domain.vo.FileUploadVO;
import com.ecommerce.file.service.FileStorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "文件上传")
@RestController
@RequestMapping("/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @ApiOperation("上传文件")
    @PostMapping("/upload")
    public R<FileUploadVO> upload(
            @ApiParam(value = "文件", required = true)
            @RequestParam("file") MultipartFile file,
            @ApiParam(value = "文件类型: avatar / logo / product / category / banner", required = true)
            @RequestParam("type") String type) {

        String url = fileStorageService.upload(file, type);

        FileUploadVO vo = FileUploadVO.of(
                file.getOriginalFilename(),
                url,
                type,
                file.getSize()
        );
        return R.ok(vo);
    }
}
