package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.upload.config.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@EnableConfigurationProperties(UploadProperties.class) // 导入配置文件属性类
public class UploadService {
    // FastDFS客户端注入
    @Autowired
    private FastFileStorageClient storageClient;

    // 配置文件属性类
    @Autowired
    private UploadProperties uploadProperties;

//    private static final List<String> ALLOW_TYPES = Arrays.asList("image/jpeg", "image/png", "image/web", "image/bmp");

    public String uploadImage(MultipartFile file) {
        try {
            // 校验文件
            String contentType = file.getContentType(); // 获取请求头中的文件类型
            if (!uploadProperties.getAllowTypes().contains(contentType)) {
                // 不符合的文件类型
                throw new LyException(ExceptionEnum.FILE_TYPE_INVALID);
            }

            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                // 读图片的方式得到null，说明也是无效类型
                throw new LyException(ExceptionEnum.FILE_TYPE_INVALID);
            }

            // 保存文件
//            File dest = new File("D:\\develop\\IdeaProjects\\main-frame\\leyou\\" + file.getOriginalFilename());
//            file.transferTo(dest);
//            return "http://image.leyou.com/" + file.getOriginalFilename();

            // 保存文件
//            String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")); //低效获取扩展名方式
            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), extension, null);

            return uploadProperties.getBaseUrl() + storePath.getFullPath();
        } catch (IOException e) {
            // 上传失败
            log.error("[文件上传] 文件上传失败");
            throw new LyException(ExceptionEnum.FILE_UPLOAD_ERROR);
        }
    }
}
