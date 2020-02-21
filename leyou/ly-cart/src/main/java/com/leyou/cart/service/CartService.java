package com.leyou.cart.service;

import com.leyou.auth.entiy.UserInfo;
import com.leyou.cart.interceptor.UserInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:uid:";

    /**
     * 添加商品至购物车
     * @param cart
     */
    public void addCart(Cart cart) {
        // 获取登录用户
        UserInfo userInfo = UserInterceptor.getUser();
        // key
        String key = KEY_PREFIX + userInfo.getId();
        // hashKey
        String hashKey = cart.getSkuId().toString();
        // 记录num
        Integer num = cart.getNum();

        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);
        // 判断当前购物车商品是否存在
        if (operations.hasKey(hashKey)) {
            // 存在，修改数量
            String json = operations.get(hashKey).toString();
            cart = JsonUtils.toBean(json, Cart.class);
            cart.setNum(cart.getNum() + num);
        }
        // 写回redis
        operations.put(hashKey, JsonUtils.toString(cart));
    }

    /**
     * 查询购物车
     * @return
     */
    public List<Cart> loadCarts() {
        // 获取登录用户
        UserInfo userInfo = UserInterceptor.getUser();
        // key
        String key = KEY_PREFIX + userInfo.getId();

        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);
        if (operations.keys() != null) {
            return operations.values().stream().map(o -> JsonUtils.toBean(o.toString(), Cart.class)).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 修改购物车商品数量
     * @param skuId
     * @param num
     */
    public void updateCartNum(String skuId, Integer num) {
        // 获取登录用户
        UserInfo userInfo = UserInterceptor.getUser();
        // key
        String key = KEY_PREFIX + userInfo.getId();

        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);
        if (!operations.hasKey(skuId)) {
            throw new LyException(ExceptionEnum.CART_NOT_FOUND);
        }
        Cart cart = JsonUtils.toBean(operations.get(skuId).toString(), Cart.class);
        cart.setNum(num);
        operations.put(skuId, JsonUtils.toString(cart));
    }

    /**
     * 删除购物车商品
     * @param skuId
     */
    public void deleteCart(String skuId) {
        // 获取登录用户
        UserInfo userInfo = UserInterceptor.getUser();
        // key
        String key = KEY_PREFIX + userInfo.getId();

        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);
        if (!operations.hasKey(skuId)) {
            throw new LyException(ExceptionEnum.CART_NOT_FOUND);
        }
        operations.delete(skuId);
    }
}
