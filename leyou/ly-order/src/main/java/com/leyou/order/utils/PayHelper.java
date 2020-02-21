package com.leyou.order.utils;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.order.config.PayConfig;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.github.wxpay.sdk.WXPayConstants.*;

@Slf4j
@Component
public class PayHelper {
    @Autowired
    private PayConfig payConfig;

    @Autowired
    private WXPay wxPay;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    private static final String TRADE_TYPE = "NATIVE";

    /**
     * 获取支付的url链接
     * @param orderId
     * @param desc
     * @param totalPay
     * @return
     */
    public String getPayUrl(Long orderId, Long totalPay, String desc) {
        try {
            //准备请求参数
            Map<String, String> data = new HashMap<>();
            data.put("body", desc);
            data.put("out_trade_no", orderId.toString());
            data.put("total_fee", totalPay.toString());
            data.put("spbill_create_ip", "123.12.12.123");
            data.put("notify_url", payConfig.getNotifyUrl());
            data.put("trade_type", TRADE_TYPE);
            Map<String, String> resp = wxPay.unifiedOrder(data);
            //校验通信状态
            isConnectSuccess(resp);
            //校验业务状态
            isBusinessSuccess(resp);
            // 校验签名
            isSignatureValid(resp);
            return resp.get("code_url");
        } catch (Exception e) {
            log.error("【微信支付】微信下单失败。", e);
            throw new LyException(ExceptionEnum.ORDER_WX_PAY_ERROR);
        }
    }

    public void isBusinessSuccess(Map<String, String> resp) {
        //校验业务标识
        if ("FAIL".equals(resp.get("result_code"))) {
            log.error("【微信支付】微信下单失败，原因：{}", resp.get("err_code_des"));
            throw new LyException(ExceptionEnum.ORDER_WX_PAY_ERROR);
        }
    }

    public void isConnectSuccess(Map<String, String> resp) {
        //校验通信标识
        if ("FAIL".equals(resp.get("return_code"))) {
            log.error("【微信支付】微信支付通信失败，原因：{}", resp.get("return_msg"));
            throw new LyException(ExceptionEnum.ORDER_WX_CONNECTION_ERROR);
        }
    }

    public void isSignatureValid(Map<String, String> resp) {
        try {
            boolean boo1 = WXPayUtil.isSignatureValid(resp, payConfig.getKey(), WXPayConstants.SignType.HMACSHA256);
            boolean boo2 = WXPayUtil.isSignatureValid(resp, payConfig.getKey(), WXPayConstants.SignType.MD5);
            if (!boo1 && !boo2) {
                //签名有误
                log.error("【微信支付】微信签名无效。");
                throw new LyException(ExceptionEnum.ORDER_WX_SIGNATURE_INVALID);
            }
        } catch (Exception e) {
            log.error("【微信支付】微信签名无效。",e);
            throw new LyException(ExceptionEnum.ORDER_WX_SIGNATURE_INVALID);
        }
    }

    public PayState queryPayState(Long orderId) {
        try {
            //准备请求参数
            Map<String, String> data = new HashMap<>();
            data.put("out_trade_no", orderId.toString());
            Map<String, String> result = wxPay.orderQuery(data);

            // 1.校验通信标识
            isConnectSuccess(result);

            // 2.校验签名标识
            isSignatureValid(result);

            // 3.校验金额是否正确
            String totalFeeStr = result.get("total_fee");
            String outTrade = result.get("out_trade_no");
            if (StringUtils.isBlank(totalFeeStr) || StringUtils.isBlank(outTrade)) {
                throw new LyException(ExceptionEnum.ORDER_PARAM_INVALID);
            }
            // 3.1获取结果中的金额
            Long totalFee = Long.valueOf(totalFeeStr);
            // 3.2获取订单金额
            Order order = orderMapper.selectByPrimaryKey(orderId);
            if (totalFee != /*order.getTotalPay()*/ 1L) {
                // 金额不符
                throw new LyException(ExceptionEnum.ORDER_PARAM_INVALID);
            }

            /**
             * SUCCESS—支付成功

             REFUND—转入退款

             NOTPAY—未支付

             CLOSED—已关闭

             REVOKED—已撤销（付款码支付）

             USERPAYING--用户支付中（付款码支付）

             PAYERROR--支付失败
             */

            String tradeState = result.get("trade_state");
            if (SUCCESS.equals(tradeState)) {
                // 支付成功
                // 4.修改订单状态
                OrderStatus orderStatus = new OrderStatus();
                orderStatus.setOrderId(orderId);
                orderStatus.setStatus(OrderStatusEnum.PAYED.value());
                orderStatus.setPaymentTime(new Date());
                int count = statusMapper.updateByPrimaryKeySelective(orderStatus);
                if (count != 1) {
                    throw new LyException(ExceptionEnum.ORDER_UPDATE_STATUS_ERROR);
                }
                // 返回成功
                return PayState.SUCCESS;
            }
            if ("NOTPAY".equals(tradeState) || "USERPAYING".equals(tradeState)) {
                return PayState.NOT_PAY;
            }

            // 其他情况返回失败
            return PayState.FAIL;
        }catch (Exception e) {
            return PayState.NOT_PAY;
        }
    }
}
