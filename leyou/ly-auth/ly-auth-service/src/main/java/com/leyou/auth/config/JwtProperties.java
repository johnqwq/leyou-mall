package com.leyou.auth.config;

import com.leyou.auth.utils.JwtUtils;
import com.leyou.auth.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@ConfigurationProperties(prefix = "ly.jwt")
@Data
public class JwtProperties {
    private String secret; // 登录校验的密钥
    private String pubKeyPath; // 公钥地址
    private String priKeyPath; // 私钥地址
    private int expire; // 过期时间,单位分钟

    private PublicKey publicKey; // 公钥
    private PrivateKey privateKey; // 私钥

    @PostConstruct
    private void init() throws Exception {
        // 如果不存在则先创建
        File pubPath = new File(pubKeyPath);
        File priPath = new File(priKeyPath);
        if (!pubPath.exists() && !priPath.exists()) {
            RsaUtils.generateKey(pubKeyPath, priKeyPath, secret);
        }

        // 获取公钥和私钥
        publicKey = RsaUtils.getPublicKey(pubKeyPath);
        privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }
}
