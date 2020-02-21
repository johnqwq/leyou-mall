package com.leyou.search.client;

import com.leyou.item.api.GoodsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * elasticsearch需要向服务模块请求服务，于是用到我们之前学的feign
 * 而如果按照我们之前的用法写接口，一来服务有几个我们就要写几个，而来调用非需要知道接口的路径
 * 为了解决这个问题，我们可以把这样的接口放在ly-item-interface，然后我们去继承这样的接口，再加上feign的注解
 */
@FeignClient("item-service")
public interface GoodsClient extends GoodsApi {

}
