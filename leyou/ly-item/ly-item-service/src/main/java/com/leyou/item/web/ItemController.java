package com.leyou.item.web;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.pojo.Item;
import com.leyou.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/item")
public class ItemController {
    @Autowired
    private ItemService itemService;

    @PostMapping
    // ResponseEntity：设置响应头中的内容
    public ResponseEntity<Item> saveItem(Item item) {
        if (item.getPrice() == null) {
            // 如果要回ResponseEntity的话，Item没法携带错误信息。所以我们选择抛出异常，然后用异常拦截器拦截并回复错误信息
            // 用内置的异常也不太好，只能携带错误信息，无法携带错误状态码。因此我们用自定义异常
            // 写一个通用异常处理去处理自定义异常，将异常信息传递给前端
            throw new LyException(ExceptionEnum.PRICE_CANNOT_BE_NULL);
        }
        item = itemService.saveItem(item);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(item);
    }
}
