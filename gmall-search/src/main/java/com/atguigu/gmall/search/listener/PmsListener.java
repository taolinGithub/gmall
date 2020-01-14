package com.atguigu.gmall.search.listener;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PmsListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GoodsRepository repository;


    //指定绑定信息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "GMALL-SEARCH-QUEUE",durable = "true"),  //队列信息
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),  //交换机信息
            key = {"item.insert"}
    ))
    public void listener(Long spuId){
        Resp<List<SkuInfoEntity>> listResp = pmsClient.querySkuBySpuId(spuId);
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
                Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.querySpuById(spuId);
                SpuInfoEntity spuEntity = spuInfoEntityResp.getData();
                goods.setCreateTime(spuEntity.getCreateTime());
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
    }
}
