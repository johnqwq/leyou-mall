package com.leyou.page.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.pojo.*;
import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

@Slf4j
@Service
public class PageService {
    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private TemplateEngine templateEngine;

    public Map<String, Object> loadModel(Long spuId) {
        Map<String, Object> model = new HashMap<>();
        Spu spu = goodsClient.querySpuBySpuId(spuId);
        List<Category> categoryList = categoryClient.queryCategoryByCids(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        Brand brand = brandClient.queryBrandByBid(spu.getBrandId());
        List<SpecGroup> specGroupList = specificationClient.querySpecGroupByCid(spu.getCid3());
        List<SpecParam> specParamList = specificationClient.querySpecParam(null, spu.getCid3(), null);


        // 组装specGroup和specParam
        // 先把规格参数变成map，map的key是规格组id，map的值是组下所有的参数
        Map<Long, List<SpecParam>> map = new HashMap<>();
        // 设置paramMap
        Map<Long, String> paramMap = new HashMap<>();
        for (SpecParam param : specParamList) {
            if (!map.containsKey(param.getGroupId())) {
                map.put(param.getGroupId(), new ArrayList<>());
            }
            map.get(param.getGroupId()).add(param);

            // 组装paramMap
            paramMap.put(param.getId(), param.getName());
        }
        // 填充param到group
        for (SpecGroup group : specGroupList) {
            group.setParams(map.get(group.getId()));
        }


        model.put("spu", spu);
        model.put("skus", spu.getSkus());
        model.put("detail", spu.getSpuDetail());
        model.put("categories", categoryList);
        model.put("brand", brand);
        model.put("specs", specGroupList);

//        model.put("paramMap", paramMap);
        return model;
    }

    public void createHtml(Long spuId) {
        // 上下文
        Context context = new Context();
        context.setVariables(loadModel(spuId));
        // 输出流
        File dest = new File("D:\\develop\\IdeaProjects\\main-frame\\leyou", spuId + ".html");

        // 如果已经存在相应的静态页面则先删除
        if (dest.exists()) {
            dest.delete();
        }

        try(PrintWriter writer = new PrintWriter(dest, "UTF-8")) {
            // 生成HTML
            templateEngine.process("item", context, writer);
        }catch (Exception e) {
            log.error("[静态页服务] 生成静态页异常！", e);
        }
    }

    public void deleteHtml(Long spuId) {
        File dest = new File("D:\\develop\\IdeaProjects\\main-frame\\leyou", spuId + ".html");
        // 删除静态页面
        if (dest.exists()) {
            dest.delete();
        }
    }
}
