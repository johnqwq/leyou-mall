package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.*;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private GoodsRepository repository;

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;
    
    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private GoodsClient goodsClient;

    public Goods buildGoods(Spu spu) {
        Long id = spu.getId();
        // 1.搜索字段
        // 查询分类
        List<Category> categoryList = categoryClient.queryCategoryByCids(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if (CollectionUtils.isEmpty(categoryList)) {
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        List<String> nameList = categoryList.stream().map(Category::getName).collect(Collectors.toList());
        // 查询品牌
        Brand brand = brandClient.queryBrandByBid(spu.getBrandId());
        if (brand == null) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }

        // 拼接
        String all = spu.getTitle() + StringUtils.join(nameList, " ") + brand.getName();

        // 2.所有sku的价格集合  3.所有sku的集合的json格式
        List<Sku> skuList = goodsClient.querySkusById(id);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        //对sku进行处理
        List<Map<String, Object>> skus = new ArrayList<>();

        // 价格集合
        List<Long> skuPriceList = new ArrayList<>();
        for (Sku sku : skuList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("price", sku.getPrice());
            map.put("image", StringUtils.substringBefore(sku.getImages(), ",")); // 只要第一张图片
            skus.add(map);

            // 处理价格
            skuPriceList.add(sku.getPrice());
        }

        // 4. 所有的可搜索的规格参数
        // 查询规格参数
        List<SpecParam> specParamList = specClient.querySpecParam(null, spu.getCid3(), true);
        if (CollectionUtils.isEmpty(specParamList)) {
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        // 查询商品详情
        SpuDetail spuDetail = goodsClient.querySpuDetailById(id);
        // 获取通用规格参数
        String genericSpec = spuDetail.getGenericSpec();
        Map<Long, String> genericMap = JsonUtils.toMap(genericSpec, Long.class, String.class);
        // 获取特有规格参数
        String specialSpec = spuDetail.getSpecialSpec();
        Map<Long, List<String>> specialMap = JsonUtils.nativeRead(specialSpec, new TypeReference<Map<Long, List<String>>>() {});

        // 规格参数，key是规格参数的名字，值是规格参数的值
        Map<String, Object> map = new HashMap<>();
        for (SpecParam param : specParamList) {
            // 规格名称
            String key = param.getName();
            Object value;
            // 判断是否是通用规格
            if (param.getGeneric()) {
                value = genericMap.get(param.getId());
                // 过滤参数中有一类比较特殊，就是数值区间
                // map属性主要用于搜索，如果在这里不先把数值类型数据分段，则后期搜索匹配更麻烦
                if (param.getNumeric()) {
                    // 处理成段
                    value = chooseSegment(value.toString(), param);
                }
            }else {
                value = specialMap.get(param.getId());
            }
            // 存入map
            map.put(key, value);
        }

        // 构建goods对象
        Goods goods = new Goods();
        goods.setId(id);
        goods.setAll(all); // 搜索字段，包含标题，分类，品牌，规格等
        goods.setSubTitle(spu.getSubTitle());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setPrice(skuPriceList); // 所有sku的价格集合
        goods.setSkus(JsonUtils.toString(skus)); // 所有sku的集合的json格式
        goods.setSpecs(map); // 所有的可搜索的规格参数

        return goods;
    }

    /**
     * 对数值型数据进行分段处理(数值转为某个区间段)
     * @param value
     * @param p
     * @return
     */
    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public PageResult<Goods> search(SearchRequest searchRequest) {
        String key = searchRequest.getKey();
        // 判断是否有搜索条件，如果没有，直接返回null，不允许搜索全部商品
        if (StringUtils.isBlank(key)) {
            return null;
        }

        Integer page = searchRequest.getPage() - 1; // queryBuilder的查询分页默认从第0页开始，所以要对传过来的参数减1
        Integer size = searchRequest.getSize();

        // 创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 0.结果过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));
        // 1.分页
        queryBuilder.withPageable(PageRequest.of(page, size));
        // 2.搜索条件
//        QueryBuilder basicQuery = QueryBuilders.matchQuery("all", key);
        // 根据用户点击的参数条件设置相应的搜索条件
        QueryBuilder basicQuery;
        if (searchRequest.getFilter() != null) {
            basicQuery = buildBasicQuery(searchRequest);
        }else {
            basicQuery = QueryBuilders.matchQuery("all", key);
        }
        queryBuilder.withQuery(basicQuery);

        // 3.聚合
        // 3.1聚合分类
        String categoryAggName = "category_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        // 3.2聚合品牌
        String brandAggName = "brand_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 4.查询
//        Page<Goods> result = repository.search(queryBuilder.build());
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 5.解析结果
        // 5.解析分页结果
        long total = result.getTotalElements();
        int totalPages = result.getTotalPages();
        List<Goods> goodsList = result.getContent();
        // 5.2解析聚合结果
        Aggregations aggs = result.getAggregations();
        List<Category> categoryList = parseCategoryAgg(aggs.get(categoryAggName));
        List<Brand> brandList = parseBrandAgg(aggs.get(brandAggName));

        // 6.完成规格参数聚合
        List<Map<String,Object>> specs = new ArrayList<>();
        if (categoryList != null && categoryList.size() == 1) {
            // 商品分类存在且数量为1，可以聚合规格参数
            specs = buildSpecificationAgg(categoryList.get(0).getId(), basicQuery);
        }

//        return new PageResult<>(total, totalPages, goodsList);
//        return new SearchResult(total, totalPages, goodsList, categoryList, brandList);
        return new SearchResult(total, totalPages, goodsList, categoryList, brandList, specs);
    }

    /**
     * 依据filter为查询添加过滤条件
     * @param searchRequest
     * @return
     */
    private QueryBuilder buildBasicQuery(SearchRequest searchRequest) {
        // 创建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 查询条件
        queryBuilder.must(QueryBuilders.matchQuery("all", searchRequest.getKey()));
        // 过滤条件
        Map<String, String> filter = searchRequest.getFilter();
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            // 处理key； 当key不是cid3或brandId时查询的FIELD是 "specs."+key+".keyword"
            if (!"cid3".equals(key) && !"brandId".equals(key)) {
                key = "specs."+key+".keyword";
            }
            queryBuilder.filter(QueryBuilders.termQuery(key, entry.getValue()));
        }
        return queryBuilder;
    }

    /**
     * 根据id和查询条件 完成 规格参数聚合
     * @param id
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> buildSpecificationAgg(Long id, QueryBuilder basicQuery) {
        List<Map<String, Object>> specs = new ArrayList<>();
        // 1.查询需要聚合的规格参数
        List<SpecParam> specParamList = specClient.querySpecParam(null, id, true);
        // 2.聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 2.1带上查询条件
        queryBuilder.withQuery(basicQuery);
        // 2.2遍历规格参数聚合
        for (SpecParam specParam : specParamList) {
            String name = specParam.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs."+name+".keyword"));
        }
        // 3.获取结果
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        // 4.解析结果
        Aggregations aggs = result.getAggregations();
        for (SpecParam specParam : specParamList) {
            // 规格参数名
            String name = specParam.getName();
            StringTerms terms = aggs.get(name);
            List<String> options = terms.getBuckets().stream().map(b -> b.getKeyAsString()).collect(Collectors.toList());

            // 准备map
            Map<String, Object> map = new HashMap<>();
            map.put("k", name);
            map.put("options", options);
            specs.add(map);
        }
        return specs;
    }

    /**
     * 解析聚合结果，将得到的ids进行商品分类的查询并返回
     * @param terms
     * @return
     */
    private List<Category> parseCategoryAgg(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets().stream().map(bucket -> bucket.getKeyAsNumber().longValue()).collect(Collectors.toList());
            return categoryClient.queryCategoryByCids(ids);
        }catch (Exception e) {
            return null;
        }
    }

    private List<Brand> parseBrandAgg(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets().stream().map(bucket -> bucket.getKeyAsNumber().longValue()).collect(Collectors.toList());
            return brandClient.queryBrandByBids(ids);
        }catch (Exception e) {
            return null;
        }
    }

    public void createOrUpdateIndex(Long spuId) {
        // 查询spu
        Spu spu = goodsClient.querySpuBySpuId(spuId);
        // 构建goods
        Goods goods = buildGoods(spu);
        // 存入索引库
        repository.save(goods);
    }

    public void delete(Long spuId) {
        repository.deleteById(spuId);
    }
}
