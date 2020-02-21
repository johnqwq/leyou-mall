package com.leyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 上传组件启动器
 */
@SpringBootApplication
@EnableDiscoveryClient
public class LyUpload {
    public static void main(String[] args) {
        SpringApplication.run(LyUpload.class);
    }
}
