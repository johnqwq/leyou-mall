package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoodsService {
    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<Spu> querySpuByPage(String key, Boolean saleable, Integer page, Integer rows) {
        // 分页
        PageHelper.startPage(page, rows);

        // 搜索过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%"+ key +"%");
        }
        // 上下架过滤
        if (saleable != null) {
            // 如果传递了saleable参数，则筛选相应的数据
            criteria.andEqualTo("saleable", saleable);
        }

        // 默认排序
        example.setOrderByClause("last_update_time DESC");

        // 查询
        List<Spu> spus = spuMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(spus)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        // 查询各个spu的商品分类名、品牌名
        loadCategoryAndBrandName(spus);

        // 封装
        PageInfo<Spu> info = new PageInfo<>(spus);
        return new PageResult<>(info.getTotal(), spus);
    }

    private void loadCategoryAndBrandName(List<Spu> spus) {
        for (Spu spu : spus) {
            // 1.获取商品分类名
//            List<Category> list = categoryService.queryCategoryListByCid(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

            // 复习一下流的使用，好久没写了，好一会才记起来一点
            // 同时再复习一下方法引用，也是忘了很多
            // 最后collect的地方已经很模糊了
            List<String> names = categoryService.queryCategoryByCids(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream().map(Category::getName).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names, "/")); // 这里的集合组字符串会比那种字符串逐个拼起来的方式要高效

            // 2.获取品牌名
            spu.setBname(brandService.queryBrandByBid(spu.getBrandId()).getName());
        }
    }

    /**
     * 商品新增
     * @param spu
     * @return
     */
    @Transactional
    public void saveGoods(Spu spu) {
        // 预处理
        spu.setId(null);
        spu.setSaleable(true);
        spu.setValid(false);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());

        // 1.保存spu
        int count = spuMapper.insert(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }

        // 2.保存spudetail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        count = spuDetailMapper.insert(spuDetail);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }

        // 抽取新增sku和stock，以便修改商品时也能使用
        saveSkuAndStock(spu);

        // 5.发送mq消息(通知其他服务及时更新)
        amqpTemplate.convertAndSend("item.insert", spu.getId());
    }

    private void saveSkuAndStock(Spu spu) {
        int count;
        // 定义库存集合(插入库存的时候使用)
        List<Stock> list = new ArrayList<>();
        // 3.保存sku
        List<Sku> skus = spu.getSkus();
        for (Sku sku : skus) {
            sku.setSpuId(spu.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());

            count = skuMapper.insert(sku);
            if (count != 1) {
                throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
            }

            // 提取新增操作，批量插入
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            list.add(stock);
        }

        // 4.批量新增库存
        count = stockMapper.insertList(list);
        if (count != list.size()) {
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
    }

    /**
     * 查询商品详情SpuDetail
     * @return
     */
    public SpuDetail querySpuDetailById(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (spuDetail == null) {
            throw new LyException(ExceptionEnum.GOODS_DETAIL_NOT_FOUND);
        }
        return spuDetail;
    }

    /**
     * 查询商品Sku
     * @return
     */
    public List<Sku> querySkusById(Long spuId) {
        // 查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }

        // 查询sku中的stock
//        for (Sku s : skuList) {
//            Stock stock = stockMapper.selectByPrimaryKey(s.getId());
//            if (stock == null) {
//                throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
//            }
//            s.setStock(stock.getStock());
//        }

        // 查询sku中的stock(流+map版)
        List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
        // 根据stock集合批量查询
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stockList)) {
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }

        // 我们把stock变成一个map，其key是sku的id，值是库存值
        Map<Long, Integer> stockMap = stockList.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skuList.forEach(s -> s.setStock(stockMap.get(s.getId())));

        return skuList;
    }

    /**
     * 修改商品
     * @param spu
     * @return
     */
    @Transactional
    public void updateGoods(Spu spu) {
        if (spu.getId() == null) {
            throw new LyException(ExceptionEnum.GOODS_ID_CANNOT_BE_NULL);
        }

        // 1.修改sku
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        List<Sku> skuList = skuMapper.select(sku);
        int count;
        if (!CollectionUtils.isEmpty(skuList)) {
            // 删除原有的sku     delete：根据sku中的非空条件删除所有找到的sku
            count = skuMapper.delete(sku);
            if (count != skuList.size()) {
                throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
            }

            // 删除stock
            List<Long> skuIds = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            count = stockMapper.deleteByIdList(skuIds);
            if (count != skuIds.size()) {
                throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
            }
        }

        // 2.修改sku
        // 预处理
        spu.setValid(null);
        spu.setSaleable(null); // 这里应该是根据页面传递的内容来修改的，但是老师设置成null默认不修改，原因未知
        spu.setLastUpdateTime(new Date());
        spu.setCreateTime(null);

        // 选择性更新修改(更新非空条件的属性)
        count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }

        // 3.修改SpuDetail
        SpuDetail spuDetail = spu.getSpuDetail();
        count = spuDetailMapper.updateByPrimaryKeySelective(spuDetail);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }

        // 4.新增sku和stock
        saveSkuAndStock(spu);

        // 5.发送mq消息(通知其他服务及时更新)
        amqpTemplate.convertAndSend("item.update", spu.getId());
    }

    public Spu querySpuBySpuId(Long spuId) {
        // 查询spu
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        // 查询SpuDetail
        spu.setSpuDetail(querySpuDetailById(spuId));
        // 查询Skus
        spu.setSkus(querySkusById(spuId));
        return spu;
    }

    public List<Sku> querySkusByIds(List<Long> ids) {
        List<Sku> skuList = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }

        // 根据stock集合批量查询
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stockList)) {
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }

        // 我们把stock变成一个map，其key是sku的id，值是库存值
        Map<Long, Integer> stockMap = stockList.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skuList.forEach(s -> s.setStock(stockMap.get(s.getId())));

        return skuList;
    }

    /**
     * 同步结构减库存
     * @return
     */
    @Transactional
    public void decreaseStock(List<CartDTO> carts) {
        for (CartDTO cart : carts) {
            int count = stockMapper.decreaseStock(cart.getSkuId(), cart.getNum());
            if (count != 1) {
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
