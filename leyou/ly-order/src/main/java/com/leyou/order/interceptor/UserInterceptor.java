package com.leyou.order.interceptor;

import com.leyou.auth.entiy.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.order.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 很多接口都需要进行登录，我们直接编写SpringMVC拦截器，进行统一登录校验。
 * 同时，我们还要把解析得到的用户信息保存起来，以便后续的接口可以使用
 */
@Slf4j
public class UserInterceptor implements HandlerInterceptor {
    private JwtProperties prop;

    // 以线程作为key，存放用户信息。不同线程间不共享，避免出现线程安全问题
    private static final ThreadLocal<UserInfo> tl = new ThreadLocal<>();

    public UserInterceptor(JwtProperties prop) {
        this.prop = prop;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取cookie中的token
        String token = CookieUtils.getCookieValue(request, prop.getCookieName());
        try {
            // 解析token
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            // 传递user
            tl.set(userInfo);
            //放行
            return true;
        }catch (Exception e) {
            log.error("[购物车服务] 解析用户身份失败.", e);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 最后用完数据，一定要清空，否则越积越多
        tl.remove();
    }

    /**
     * 方便从拦截器中获取线程对应的用户信息
     */
    public static UserInfo getUser() {
        return tl.get();
    }
}
