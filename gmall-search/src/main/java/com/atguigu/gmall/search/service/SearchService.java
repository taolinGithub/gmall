package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParam;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVO;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.tomcat.util.descriptor.InputSourceUtil;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient highLevelClient;

    public SearchResponseVo search(SearchParam searchParam){
        SearchResponse searchResponse =null;
        try {
             searchResponse = highLevelClient.search(new SearchRequest(new String[]{"goods"}, builderDSL(searchParam)), RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SearchResponseVo responseVo = parseSearchResult(searchResponse);
        responseVo.setPageNum(searchParam.getPageNum());
        responseVo.setPageSize(searchParam.getPageSize());

        return responseVo;
    }
    //解析查询结果集
    private  SearchResponseVo parseSearchResult(SearchResponse searchResponse){
        SearchResponseVo responseVo = new SearchResponseVo();
        //总记录数
        responseVo.setTotal(searchResponse.getHits().getTotalHits());
        //查询结果集的封装
        SearchHit[] hits = searchResponse.getHits().getHits();
        List<Goods> goodsList =new ArrayList<>();
        for (SearchHit hit : hits) {
            //获取_source反序列化为goods
            String goodsJson = hit.getSourceAsString();
            Goods goods = JSON.parseObject(goodsJson, Goods.class);
            //获取高亮结果进行覆盖
            goods.setSkuTitle( hit.getHighlightFields().get("skuTitle").getFragments()[0].string());
            goodsList.add(goods);
           // SearchResponseAttrVO
        }
        responseVo.setProducts(goodsList);

        //解析品牌的聚合结果集

        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        ParsedLongTerms terms = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        SearchResponseAttrVO brandVO = new SearchResponseAttrVO();
            brandVO.setName("品牌");
            brandVO.setProductAttributeId(null);
            //获取桶

        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        if(!CollectionUtils.isEmpty(buckets)){
            //获取桶
            List<String> brandValue = buckets.stream().map(bucket -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", ((Terms.Bucket) bucket).getKeyAsNumber());
                Map<String, Aggregation> stringAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                ParsedStringTerms brandNameAgg =(ParsedStringTerms) stringAggregationMap.get("brandNameAgg");
                map.put("name",brandNameAgg.getBuckets().get(0).getKeyAsString());
                        return JSON.toJSONString(map);
                    }
            ).collect(Collectors.toList());
            brandVO.setValue(brandValue);
            responseVo.setBrand(brandVO);
        }


        //解析分类的聚合结果集
        ParsedLongTerms termss = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        SearchResponseAttrVO CateVO = new SearchResponseAttrVO();
        CateVO.setName("分类");
        CateVO.setProductAttributeId(null);
        //获取桶

        List<? extends Terms.Bucket> Catebuckets = termss.getBuckets();
        if(!CollectionUtils.isEmpty(Catebuckets)){
            //获取桶
            List<String> CateValue = Catebuckets.stream().map(bucket -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", ((Terms.Bucket) bucket).getKeyAsNumber());
                        Map<String, Aggregation> stringAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                        ParsedStringTerms brandNameAgg =(ParsedStringTerms) stringAggregationMap.get("categoryNameAgg");
                        map.put("name",brandNameAgg.getBuckets().get(0).getKeyAsString());
                        return JSON.toJSONString(map);
                    }
            ).collect(Collectors.toList());
            CateVO.setValue(CateValue);
            responseVo.setCatelog(CateVO);
        }



        //规格参数
        ParsedNested attrsAgg = (ParsedNested)aggregationMap.get("attrsAgg");
        ParsedLongTerms attrId = (ParsedLongTerms)attrsAgg.getAggregations().asMap().get("attrIdAgg");
        List<? extends Terms.Bucket> idBuckets = attrId.getBuckets();
        List<SearchResponseAttrVO> attrVOS =idBuckets.stream().map(bucket->{
            SearchResponseAttrVO searchResponseAttrVO = new SearchResponseAttrVO();
            searchResponseAttrVO.setProductAttributeId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
           //获取规格参数名子聚合
            ParsedStringTerms attrNameAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
            searchResponseAttrVO.setName(attrNameAgg.getBuckets().get(0).getKeyAsString());
            ////获取规格参数值子聚合
            ParsedStringTerms attrValueAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
            List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
            List<String> values = attrValueAggBuckets.stream().map(bucket1 -> {
                return ((Terms.Bucket) bucket1).getKeyAsString();
            }).collect(Collectors.toList());

            searchResponseAttrVO.setValue(values);

                return searchResponseAttrVO;
        }).collect(Collectors.toList());
        responseVo.setAttrs(attrVOS);

        return responseVo;
    }





    public SearchSourceBuilder builderDSL(SearchParam searchParam){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.fetchSource(new String[]{"skuId","skuTitle","skuSubTitle","price","defaultImage"},null);
        //1构建查询条件
        //1.1构建匹配查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        String key = searchParam.getKey();
        if(StringUtils.isEmpty(key)){
            return sourceBuilder;
        }
        boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle",key).operator(Operator.AND));
        //1.2构建过滤条件
        Long[] brand = searchParam.getBrand();
        if(brand!=null&&brand.length!=0){
            //1.2.1品牌的过滤
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brand));
        }
        //1.2.2分类的过滤
        Long[] catelog3 = searchParam.getCatelog3();
        if(catelog3!=null&&catelog3.length!=0){
            //1.2.1品牌的过滤
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",catelog3));
        }
        //1.2.3价格区间的过滤
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
        Double priceFrom = searchParam.getPriceFrom();
        if(priceFrom!=null){
           rangeQueryBuilder.gte(priceFrom);
        }
        Double priceTo = searchParam.getPriceTo();
        if(priceTo!=null){
           rangeQueryBuilder.lte(priceTo);
        }
        boolQueryBuilder.filter(rangeQueryBuilder);
        //1.2.4 规格属性的过滤
        List<String> props = searchParam.getProps();
        if(!CollectionUtils.isEmpty(props)){
            props.forEach(prop->{
                String[] attr = StringUtils.split(prop, ":");
                if(attr!=null &&attr.length==2){
                    String Id=attr[0];
                    String[] values = StringUtils.split(attr[1], "-");
                    BoolQueryBuilder boolQueryBuilder1 = QueryBuilders.boolQuery();
                    boolQueryBuilder1.must(QueryBuilders.termQuery("attrs.attrId",Id));
                    boolQueryBuilder1.must(QueryBuilders.termsQuery("attrs.attrValue",values));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs",boolQueryBuilder1, ScoreMode.None));
                }
            });
        }
        sourceBuilder.query(boolQueryBuilder);
        //2构建排序
        String order = searchParam.getOrder();
        if(!StringUtils.isEmpty(order)){
            String[] orderSplit = StringUtils.split(order,":");

        if(orderSplit!=null&&orderSplit.length==2){
               String orderId=orderSplit[0];
               String orderBy=orderSplit[1];
               switch (orderId){
                   case "0": orderId="_score"; break;
                   case "1": orderId="sale";   break;
                   case "2": orderId="price";  break;
                   default: orderId="_score";  break;
               }
               sourceBuilder.sort(orderId,orderBy.equals("asc")? SortOrder.ASC :SortOrder.DESC);
           }
        }

        //3.构建分页
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        sourceBuilder.from((pageNum-1)*pageSize);
        sourceBuilder.size(pageSize);

        //4.构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("skuTitle").preTags("<span style='color:red';>").postTags("</span>"));

        //5构建聚合
        //5.1品牌的聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId").subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName")));


        //5.2分类的聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId").subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));

        //5.3规格属性的聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrsAgg","attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName")).
                                subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")
                                )
             )
        );

        System.out.println(sourceBuilder);
        return sourceBuilder;
    }
}