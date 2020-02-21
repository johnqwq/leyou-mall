package com.leyou.item.web;

import com.leyou.item.pojo.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 根据pid查询分类集合
     * @param pid
     * @return
     */
    @GetMapping("/list")
    public ResponseEntity<List<Category>> queryCategoryListByPid(@RequestParam("pid") Long pid) {
//        return ResponseEntity.status(HttpStatus.OK).body();
        return ResponseEntity.ok(categoryService.queryCategoryListByPid(pid));
    }

    /**
     * 根据商品分类id查询名称
     * @param cids 要查询的分类id集合
     * @return 多个名称的集合
     */
    @GetMapping("/list/ids")
    public ResponseEntity<List<Category>> queryCategoryByCids(@RequestParam("ids") List<Long> cids) {
        return ResponseEntity.ok(categoryService.queryCategoryByCids(cids));
    }
}
