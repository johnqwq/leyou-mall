package com.leyou.item.mapper;

import com.leyou.common.mapper.BaseMapper;
import com.leyou.item.pojo.Stock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * BaseMapper：进一步抽取的通用mapper
 */
public interface StockMapper extends BaseMapper<Stock> {
    @Update("UPDATE tb_stock SET stock=stock - #{num} WHERE sku_id=#{skuId} AND stock>=#{num}")
    int decreaseStock(@Param("skuId") Long skuId, @Param("num") Integer num);
}
