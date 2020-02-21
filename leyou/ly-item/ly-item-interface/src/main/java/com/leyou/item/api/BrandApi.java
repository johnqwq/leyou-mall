package com.leyou.item.api;

import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Brand;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface BrandApi {
    /**
     * 分页查询品牌
     * @return
     */
    @GetMapping("/brand/page")
    PageResult<Brand> queryBrandByPage(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "desc", defaultValue = "false") Boolean desc,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows
    );

    /**
     * 新增品牌
     * @return
     */
    @PostMapping
    Void saveBrand(Brand brand, @RequestParam("cids") List<Long> cids);

    /**
     * 根据cid查询品牌
     * @return
     */
    @GetMapping("/brand/cid/{cid}")
    List<Brand> queryBrandByCid(@PathVariable("cid") Long cid);

    /**
     * 根据Bid查询品牌
     * @param Bid
     * @return
     */
    @GetMapping("/brand/{id}")
    Brand queryBrandByBid(@PathVariable("id") Long Bid);

    /**
     * 根据多个id查询brand
     */
    @GetMapping("/brand/brands")
    List<Brand> queryBrandByBids(@RequestParam("ids") List<Long> bids);
}
