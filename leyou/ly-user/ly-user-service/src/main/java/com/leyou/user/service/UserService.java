package com.leyou.user.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // redis存储数据前缀
    private static final String KEY_PREFIX = "sms:phone:";

    /**
     * 实现用户数据的校验
     * @param data
     * @param type
     * @return
     */
    public Boolean checkData(String data, Integer type) {
        User record = new User();
        switch (type) {
            case 1:
                record.setUsername(data);
                break;
            case 2:
                record.setPhone(data);
                break;
            default:
                throw new LyException(ExceptionEnum.USER_DATA_TYPE_INVALID);
        }
        return userMapper.selectCount(record) == 0;
    }

    /**
     * 发送验证码到用户手机
     * @param phone
     */
    public void sendCode(String phone) {
        // 生成key
        String key = KEY_PREFIX + phone;
        // 生成随机验证码
        String code = NumberUtils.generateCode(6);
        Map<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        amqpTemplate.convertAndSend("ly.sms.exchange", "sms.verify.code", msg);

        // 保存验证码
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
    }

    /**
     * 实现用户注册功能
     * @param user
     * @param code
     */
    public void register(User user, String code) {
        // 校验验证码
        String key = KEY_PREFIX + user.getPhone();
        if (!StringUtils.equals(code, redisTemplate.opsForValue().get(key))) {
            throw new LyException(ExceptionEnum.VERIFY_CODE_INVALID);
        }

        // 生成salt  (注意这里的CodecUtils不能放在common模块里，因为加密的方式不能暴露给哪怕同一个公司的人)
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);

        // 对密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));

        // 创建时间
        user.setCreated(new Date());

        // 写入数据库
        userMapper.insert(user);
    }

    public User queryUserByUsernameAndPassword(String username, String password) {
        // 查询用户
        User record = new User();
        record.setUsername(username);
        User user = userMapper.selectOne(record);

        // 校验用户名
        if (user == null) {
            throw new LyException(ExceptionEnum.USERNAME_PASSWORD_INVALID);
        }

        // 校验密码
        if (!StringUtils.equals(user.getPassword(), CodecUtils.md5Hex(password, user.getSalt()))) {
            throw new LyException(ExceptionEnum.USERNAME_PASSWORD_INVALID);
        }

        // 用户名和密码正确
        return user;
    }
}