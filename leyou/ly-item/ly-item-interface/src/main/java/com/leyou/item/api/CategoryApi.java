package com.leyou.item.api;

import com.leyou.item.pojo.Category;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface CategoryApi {
    /**
     * 根据pid查询分类集合
     * @param pid
     * @return
     */
    @GetMapping("/category/list")
    List<Category> queryCategoryListByPid(@RequestParam("pid") Long pid);

    /**
     * 根据商品分类id查询名称
     * @param cids 要查询的分类id集合
     * @return 多个名称的集合
     */
    @GetMapping("/category/list/ids")
    List<Category> queryCategoryByCids(@RequestParam("ids") List<Long> cids);

}
