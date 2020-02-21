package com.leyou.page.web;

import com.leyou.page.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class PageController {
    @Autowired
    private PageService pageService;

    @GetMapping("/item/{spuId}.html")
    public String toItemPage(@PathVariable("spuId") Long spuId, Model model) {
        Map<String, Object> attributes = pageService.loadModel(spuId);
        model.addAllAttributes(attributes);
        return "item";
    }
}
