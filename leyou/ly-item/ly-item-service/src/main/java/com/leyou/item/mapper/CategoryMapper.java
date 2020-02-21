package com.leyou.item.mapper;

import com.leyou.item.pojo.Category;
import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.common.Mapper;

/**
 * 查询数据库
 *
 * IdListMapper用于根据多个id返回一个对象集合
 */
public interface CategoryMapper extends Mapper<Category>, IdListMapper<Category, Long> {
}
