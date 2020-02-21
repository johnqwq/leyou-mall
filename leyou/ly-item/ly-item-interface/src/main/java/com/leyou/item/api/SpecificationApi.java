package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface SpecificationApi {
    /**
     * 依据商品分类id查询该类下规格分组
     * @return
     */
    @GetMapping("/spec/groups/{cid}")
    List<SpecGroup> querySpecGroupByCid(@PathVariable("cid") Long cid);

    /**
     * 根据相应条件查询规格参数
     * @param gid   规格分组id
     * @param cid   商品分类id
     * @param searching     是否搜索条件
     * @return
     */
    @GetMapping("/spec/params")
    List<SpecParam> querySpecParam(
            @RequestParam(value = "gid", required = false) Long gid,
            @RequestParam(value = "cid", required = false) Long cid,
            @RequestParam(value = "searching", required = false) Boolean searching
    );

//    @GetMapping("/spec/group")
//    List<SpecGroup> querySpecGroupBy
}
