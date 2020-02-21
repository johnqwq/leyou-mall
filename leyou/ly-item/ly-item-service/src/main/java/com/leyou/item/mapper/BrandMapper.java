package com.leyou.item.mapper;

import com.leyou.item.pojo.Brand;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.leyou.common.mapper.BaseMapper;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * 品牌查询mapper
 */
//public interface BrandMapper extends Mapper<Brand> {
public interface BrandMapper extends BaseMapper<Brand> {
    /**
     * 保存品牌和分类的关系
     */
    @Insert("insert into tb_category_brand (category_id, brand_id) values(#{cid}, #{bid})")
    int insertCategoryBrand(@Param("cid") Long cid, @Param("bid") Long bid);

    @Select("SELECT b.`id`, b.`name`, b.`image`, b.`letter`\n" +
            " FROM tb_brand b\n" +
            " INNER JOIN tb_category_brand c ON b.`id` = c.`brand_id`\n" +
            " WHERE c.`category_id` = #{cid}")
    List<Brand> queryBrandByCid(@Param("cid") Long cid);
}