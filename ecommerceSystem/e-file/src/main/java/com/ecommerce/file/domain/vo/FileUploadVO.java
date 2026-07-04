package com.ecommerce.file.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "文件上传结果")
public class FileUploadVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件访问URL")
    private String url;

    @ApiModelProperty("文件类型")
    private String type;

    @ApiModelProperty("文件大小（字节）")
    private Long size;

    public static FileUploadVO of(String fileName, String url, String type, Long size) {
        FileUploadVO vo = new FileUploadVO();
        vo.setFileName(fileName);
        vo.setUrl(url);
        vo.setType(type);
        vo.setSize(size);
        return vo;
    }
}
