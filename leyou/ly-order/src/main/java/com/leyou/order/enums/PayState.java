package com.leyou.order.enums;

public enum PayState {
    NOT_PAY(0),// 未支付
    SUCCESS(1),// 支付成功
    FAIL(2);// 支付失败


    PayState(int value) {
        this.value = value;
    }

    int value;

    public int getValue() {
        return value;
    }
}