package com.atguigu.gmall.search;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GoodsRepository repository;



    @Test
    void contextLoads() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        //restTemplate.deleteIndex(Goods.class);
    }

    @Test
    void importData(){
        Long pageNum =1l;//当前页
        Long pageSize=100l;//一页多少数据
        do{
            QueryCondition queryCondition = new QueryCondition();
            queryCondition.setPage(pageNum);
            queryCondition.setLimit(pageSize);
            //分页查询spu
            Resp<List<SpuInfoEntity>> spuList = pmsClient.querySpuBypage(queryCondition);
            List<SpuInfoEntity> spuData = spuList.getData();
            //判断spu是否为空
            if(CollectionUtils.isEmpty(spuData)){
                return ;
            }

            //遍历spu，获取sku导入es
            spuData.forEach(spuInfoEntity -> {
                Resp<List<SkuInfoEntity>> listResp = pmsClient.querySkuBySpuId(spuInfoEntity.getId());
                List<SkuInfoEntity> list = listResp.getData();
                //把当前集合转为goods集合导入es
                if(!CollectionUtils.isEmpty(list)){
                    List<Goods> goodsList=list.stream().map(list1 ->{
                        Goods goods = new Goods();
                        goods.setSkuId(list1.getSkuId());
                        goods.setSkuTitle(list1.getSkuTitle());
                        goods.setSkuSubTitle(list1.getSkuSubtitle());
                        goods.setPrice(list1.getPrice().doubleValue());
                        goods.setDefaultImage(list1.getSkuDefaultImg());

                        goods.setSale(10l);//销量没做，默认值
                        goods.setCreateTime(spuInfoEntity.getCreateTime());
                        Resp<List<WareSkuEntity>> wareSku = wmsClient.queryWareSkuBySkuId(list1.getSkuId());
                        List<WareSkuEntity> wareSkuData = wareSku.getData();
                        if(!CollectionUtils.isEmpty(wareSkuData)){
                            goods.setStore(wareSkuData.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                        }
                                                                    //如果有一个库存大于0，此值为true
                        Resp<BrandEntity> brandEntity = pmsClient.queryBrandById(list1.getBrandId());
                        BrandEntity brand = brandEntity.getData();
                        if(brand!=null){
                            goods.setBrandId(list1.getBrandId());
                            goods.setBrandName(brand.getName());
                        }
                        Resp<CategoryEntity> categoryEntityResp = pmsClient.queryCatagoryById(list1.getCatalogId());
                        CategoryEntity categoryEntity = categoryEntityResp.getData();
                        if(categoryEntity!=null){
                            goods.setCategoryId(list1.getCatalogId());
                            goods.setCategoryName(categoryEntity.getName());
                        }

                        Resp<List<ProductAttrValueEntity>> attrValueEntityList = pmsClient.queryAttr(list1.getSpuId());
                        List<ProductAttrValueEntity> attrValueEntities = attrValueEntityList.getData();
                        List<SearchAttrValue> searchAttrValues = attrValueEntities.stream().map(attrValue -> {
                            SearchAttrValue searchAttrValue = new SearchAttrValue();
                            searchAttrValue.setAttrId(attrValue.getAttrId());
                            searchAttrValue.setAttrName(attrValue.getAttrName());
                            searchAttrValue.setAttrValue(attrValue.getAttrValue());
                            return searchAttrValue;
                        }).collect(Collectors.toList());
                        goods.setAttrs(searchAttrValues);

                        return goods;
                    }).collect(Collectors.toList());
                    repository.saveAll(goodsList);
                }

            });

            pageSize =(long)spuData.size();
            pageNum++;
        }while(pageSize ==100);
    }

}
