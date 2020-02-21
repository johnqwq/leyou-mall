package com.leyou.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author: HuYi.Zhang
 * @create: 2018-04-24 17:20
 **/
public class JsonUtils {

    public static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj.getClass() == String.class) {
            return (String) obj;
        }
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("json序列化出错：" + obj, e);
            return null;
        }
    }

    public static <T> T toBean(String json, Class<T> tClass) {
        try {
            return mapper.readValue(json, tClass);
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }

    public static <E> List<E> toList(String json, Class<E> eClass) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, eClass));
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }

    public static <K, V> Map<K, V> toMap(String json, Class<K> kClass, Class<V> vClass) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructMapType(Map.class, kClass, vClass));
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }

    public static <T> T nativeRead(String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }

    // 测试用内部类
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class User {
        String name;
        Integer age;
    }

    /**
     * 测试该工具类
     */
    public static void main(String[] args) {
        User user = new User("Jack", 21);

        // 1.toString
        String json = toString(user);
        System.out.println("json = " + json);

        // 反序列化
        // 2.toBean
        User user1 = toBean(json, User.class);
        System.out.println("user1 = " + user1);

        // 3.toList
        String json1 = "[1, 2, 3, 4, 5]";
        List<Integer> list = toList(json1, Integer.class);
        System.out.println("list = " + list);

        // 4.toMap
        String json2 = "{\"name\": \"Jack\", \"age\": \"19\"}";
        Map<String, String> map = toMap(json2, String.class, String.class);
        System.out.println("map = " + map);

        // 5.nativeRead
        String json3 = "[{\"name\": \"Jack\", \"age\": \"19\"}, {\"name\": \"Rose\", \"age\": \"18\"}]";
        // 对这种复杂对象的反序列化，可以用TypeReference的匿名内部类来传递解析需要的信息
        List<Map<String, String>> maps = nativeRead(json3, new TypeReference<List<Map<String, String>>>() {
        });
        System.out.println("maps = " + maps);
    }
}
