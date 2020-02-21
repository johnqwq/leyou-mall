package com.leyou.order.service;

import com.leyou.auth.entiy.UserInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.pojo.Sku;
import com.leyou.order.client.AddressClient;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.config.IdWorkerConfig;
import com.leyou.order.config.IdWorkerProperties;
import com.leyou.order.dto.AddressDTO;
import com.leyou.common.dto.CartDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.interceptor.UserInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.utils.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private PayHelper payHelper;

    /**
     * 提交订单
     * @param orderDTO
     * @return
     */
    @Transactional
    public Long createOrder(OrderDTO orderDTO) {
        // 1.新增订单
        Order order = new Order();
        // 1.1订单编号，基本信息
        order.setOrderId(idWorker.nextId());
        order.setPaymentType(orderDTO.getPaymentType());
        order.setPostFee(0L);
        order.setCreateTime(new Date());
        // 1.2用户信息
        UserInfo user = UserInterceptor.getUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getUsername());
        // 1.3收货人地址
        AddressDTO addressDTO = AddressClient.findById(orderDTO.getAddressId());
        order.setReceiver(addressDTO.getName());
        order.setReceiverMobile(addressDTO.getPhone());
        order.setReceiverState(addressDTO.getState());
        order.setReceiverCity(addressDTO.getCity());
        order.setReceiverDistrict(addressDTO.getDistrict());
        order.setReceiverAddress(addressDTO.getAddress());
        order.setReceiverZip(addressDTO.getZipCode());
        // 1.4金额
        Map<Long, Integer> numMap = orderDTO.getCarts().stream().collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));

        Set<Long> ids = numMap.keySet();
        List<Sku> skuList = goodsClient.querySkusByIds(new ArrayList<>(ids));
        List<OrderDetail> detailList = new ArrayList<>();
        long totalPay = 0;
        for (Sku sku : skuList) {
            totalPay += sku.getPrice() * numMap.get(sku.getId());

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(order.getOrderId());
            orderDetail.setSkuId(sku.getId());
            orderDetail.setNum(numMap.get(sku.getId()));
            orderDetail.setTitle(sku.getTitle());
            orderDetail.setPrice(sku.getPrice());
            orderDetail.setOwnSpec(sku.getOwnSpec());
            orderDetail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            detailList.add(orderDetail);
        }
        order.setTotalPay(totalPay);
        // 实付金额=总金额+邮费-优惠金额
        order.setActualPay(totalPay + order.getPostFee() - 0L);
        // 1.5order写入数据库
        int count = orderMapper.insertSelective(order); // insertSelective：没填的数据用表字段的默认值
        if (count != 1) {
            throw new LyException(ExceptionEnum.ORDER_CREATE_ERROR);
        }

        // 2.新增订单详情
        count = detailMapper.insertList(detailList);
        if (count != detailList.size()) {
            throw new LyException(ExceptionEnum.ORDER_CREATE_ERROR);
        }

        // 3.新增订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(order.getOrderId());
        orderStatus.setStatus(OrderStatusEnum.UNPAY.value());
        orderStatus.setCreateTime(new Date());
        count = statusMapper.insertSelective(orderStatus);
        if (count != 1) {
            throw new LyException(ExceptionEnum.ORDER_CREATE_ERROR);
        }

        // 4.减库存
        List<CartDTO> carts = orderDTO.getCarts();
        goodsClient.decreaseStock(carts);

        return order.getOrderId();
    }

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    public Order queryOrderById(Long id) {
        // 查询订单
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }

        // 查询订单详情
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(id);
        List<OrderDetail> orderDetailList = detailMapper.select(orderDetail);
        if (CollectionUtils.isEmpty(orderDetailList)) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        order.setOrderDetails(orderDetailList);

        // 查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(id);
        if (orderStatus == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        order.setStatus(orderStatus.getStatus());

        return order;
    }

    /**
     * 创建二维码支付链接
     * @param id
     * @return
     */
    public String createPayUrl(Long id) {
        // 查询订单
        Order order = queryOrderById(id);
        if (order.getStatus() != OrderStatusEnum.UNPAY.value()) {
            // 订单状态异常
            throw new LyException(ExceptionEnum.ORDER_STATUS_ERROR);
        }
        // 支付金额
        Long actualPay = order.getActualPay();
        // 商品描述
        String desc = order.getOrderDetails().get(0).getTitle();

        // 获取支付链接
//        payHelper.getPayUrl(id, actualPay, desc);
        String payUrl = payHelper.getPayUrl(id, 1L, desc);
        return payUrl;
    }

    public void handleNotify(Map<String, String> result) {
        // 1.校验通信标识
        payHelper.isConnectSuccess(result);

        // 2.校验签名标识
        payHelper.isSignatureValid(result);

        // 3.校验金额是否正确
        String totalFeeStr = result.get("total_fee");
        String outTrade = result.get("out_trade_no");
        if (StringUtils.isBlank(totalFeeStr) || StringUtils.isBlank(outTrade)) {
            throw new LyException(ExceptionEnum.ORDER_PARAM_INVALID);
        }
        // 3.1获取结果中的金额
        Long totalFee = Long.valueOf(totalFeeStr);
        // 3.2获取订单金额
        Long orderId = Long.valueOf(outTrade);
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (totalFee != /*order.getTotalPay()*/ 1L) {
            // 金额不符
            throw new LyException(ExceptionEnum.ORDER_PARAM_INVALID);
        }

        // 4.修改订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(orderId);
        orderStatus.setStatus(OrderStatusEnum.PAYED.value());
        orderStatus.setPaymentTime(new Date());
        int count = statusMapper.updateByPrimaryKeySelective(orderStatus);
        if (count != 1) {
            throw new LyException(ExceptionEnum.ORDER_UPDATE_STATUS_ERROR);
        }

        log.info("[订单回调] 订单支付成功，订单编号：{}", orderId);
    }

    /**
     * 主动查询订单支付状态
     * @param orderId
     * @return
     */
    public PayState queryOrderState(Long orderId) {
        // 查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        // 判断是否支付
        if (orderStatus.getStatus() != OrderStatusEnum.UNPAY.value()) {
            // 如果显示已支付，则可直接返回已支付
            return PayState.SUCCESS;
        }

        // 如果显示未支付，还需要主动到微信支付平台查询支付状态
        return payHelper.queryPayState(orderId);
    }
}
