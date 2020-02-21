package com.leyou.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 配置文件读取属性类
 */
@ConfigurationProperties(prefix = "ly.upload") // application.yml配置文件读取
@Data
public class UploadProperties {
    private String baseUrl;
    private List<String> allowTypes;
}
