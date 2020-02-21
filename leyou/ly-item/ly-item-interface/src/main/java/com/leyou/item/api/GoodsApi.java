package com.leyou.item.api;

import com.leyou.common.dto.CartDTO;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 向外提供的接口，别的模块只要继承这一接口并加上feign的注解就能向本模块发送请求调用服务
 */
public interface GoodsApi {
    /**
     * 分页查询spu
     * @param key
     * @param saleable
     * @param page
     * @param rows
     * @return
     */
    @GetMapping("/spu/page")
    PageResult<Spu> querySpuByPage(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "saleable", required = false) Boolean saleable,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows
    );

    /**
     * 查询商品详情SpuDetail
     * @return
     */
    @GetMapping("/spu/detail/{spuId}")
    // 参数是json结构要加@RequestBody
    SpuDetail querySpuDetailById(@PathVariable("spuId") Long spuId);

    /**
     * 查询商品List<Sku>
     * @return
     */
    @GetMapping("/sku/list")
    // 参数是json结构要加@RequestBody
    List<Sku> querySkusById(@RequestParam("id") Long spuId);

    /**
     * 根据spuId查询商品spu
     */
    @GetMapping("/spu")
    Spu querySpuBySpuId(@RequestParam("id") Long spuId);

    /**
     * 查询商品List<Sku>
     * @return
     */
    @GetMapping("sku/list/ids")
    List<Sku> querySkusByIds(@RequestParam("ids") List<Long> ids);

    /**
     * 减库存
     * @return
     */
    @PostMapping("/stock/decrease")
    void decreaseStock(@RequestBody List<CartDTO> carts);
}
