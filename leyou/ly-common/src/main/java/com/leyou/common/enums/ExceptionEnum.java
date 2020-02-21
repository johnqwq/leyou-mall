package com.leyou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 异常状态码+信息的枚举
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ExceptionEnum {
    PRICE_CANNOT_BE_NULL(400, "价格不能为空！"), // 实际上是在定义私有方法
    BRAND_NOT_FOUND(404, "品牌不存在"),
    CATEGORY_NOT_FOUND(404, "商品分类没查到"),
    SPEC_GROUP_NOT_FOUND(404, "商品规格分组没查到"),
    SPEC_PARAM_NOT_FOUND(404, "商品规格参数没查到"),
    GOODS_NOT_FOUND(404, "商品没查到"),
    GOODS_SAVE_ERROR(500, "商品保存失败"),
    GOODS_DETAIL_NOT_FOUND(404, "商品详情不存在"),
    GOODS_SKU_NOT_FOUND(404, "商品SKU不存在"),
    GOODS_STOCK_NOT_FOUND(404, "商品库存不存在"),
    GOODS_UPDATE_ERROR(404, "商品更新失败"),
    GOODS_ID_CANNOT_BE_NULL(400, "商品ID不能为空"),
    BRAND_SAVE_ERROR(500, "新增品牌失败"),
    FILE_TYPE_INVALID(400, "无效的文件类型"),
    FILE_UPLOAD_ERROR(500, "文件上传失败"),
    USER_DATA_TYPE_INVALID(400, "用户参数异常"),
    VERIFY_CODE_INVALID(400, "无效的验证码"),
    USERNAME_PASSWORD_INVALID(400, "用户名或密码错误"),
    TOKEN_CREATE_ERROR(500, "用户凭证生成失败"),
    UN_AUTHORIZED(403, "未授权"),
    CART_NOT_FOUND(404, "找不到购物车商品"),
    ORDER_CREATE_ERROR(500, "订单创建失败"),
    STOCK_NOT_ENOUGH(500, "库存不足"),
    ORDER_NOT_FOUND(404, "订单不存在"),
    ORDER_WX_PAY_ERROR(500, "微信下单失败"),
    ORDER_WX_CONNECTION_ERROR(500, "微信下单通信失败"),
    ORDER_WX_SIGNATURE_INVALID(400, "微信下单签名无效"),
    ORDER_STATUS_ERROR(400, "订单状态异常"),
    ORDER_PARAM_INVALID(400, "订单参数异常"),
    ORDER_UPDATE_STATUS_ERROR(500, "更新订单状态失败"),

    ; // 枚举此处分号为必须
    private Integer code;
    private String msg;
}
