package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandService {
    @Autowired
    private BrandMapper brandMapper;

    /**
     * 分页查询品牌
     * @return
     */
    public PageResult<Brand> queryBrandByPage(String key, String sortBy, Boolean desc, Integer page, Integer rows) {
        // 分页
        PageHelper.startPage(page, rows);
        // 过滤
        Example example = new Example(Brand.class);
        if (StringUtils.isNotBlank(key)) {
            // 根据关键词在name和letter里查询对应的数据
            example.createCriteria().orLike("name", "%"+key+"%").orEqualTo("letter", key.toUpperCase());
        }
        // 排序
        if (StringUtils.isNotBlank(sortBy)) {
            String orderByClause = sortBy + (desc ? " DESC" : " ASC");
            example.setOrderByClause(orderByClause);
        }
        // 查询
        List<Brand> list = brandMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(list)) {
            // 查询为空则抛出相应异常
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        // 最后返回一个结果集PageResult，需要总记录条数，所以我们利用PageInfo来计算
        PageInfo<Brand> info = new PageInfo<>(list);
        return new PageResult<>(info.getTotal(), list);
    }

    /**
     * 新增品牌
     */
    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        // 保存品牌
        brand.setId(null);
        brand.setLetter(Character.toUpperCase(brand.getLetter())); // 品牌首字母以大写形式存入
        int count = brandMapper.insert(brand);
        if (count != 1) {
            // 保存失败则抛出相应异常
            throw new LyException(ExceptionEnum.BRAND_SAVE_ERROR);
        }

        // 保存品牌和分类的关系
        for (Long cid : cids) {
            count = brandMapper.insertCategoryBrand(cid, brand.getId());
            if (count != 1) {
                throw new LyException(ExceptionEnum.BRAND_SAVE_ERROR);
            }
        }
    }

    /**
     * 根据bid查询品牌
     * @param bid
     * @return
     */
    public Brand queryBrandByBid(Long bid) {
        Brand brand = brandMapper.selectByPrimaryKey(bid);
        if (brand == null) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brand;
    }

    /**
     * 根据cid查询品牌
     * @return
     */
    public List<Brand> queryBrandByCid(Long cid) {
        List<Brand> brands = brandMapper.queryBrandByCid(cid);
        if (CollectionUtils.isEmpty(brands)) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }

    public List<Brand> queryBrandByBids(List<Long> bids) {
        List<Brand> brandList = brandMapper.selectByIdList(bids);
        if (CollectionUtils.isEmpty(brandList)) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brandList;
    }
}
