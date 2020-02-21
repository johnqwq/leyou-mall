package com.leyou.item.web;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("spec")
public class SpecificationController {

    @Autowired
    private SpecificationService specificationService;

    /**
     * 依据商品分类id查询该类下规格分组
     * @return
     */
    @GetMapping("groups/{cid}")
    public ResponseEntity<List<SpecGroup>> querySpecGroupByCid(@PathVariable("cid") Long cid){
        return ResponseEntity.ok(specificationService.querySpecGroupByCid(cid));
    }
}