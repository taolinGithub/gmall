package com.atguigu.gmall.item.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.config.ThreadPoolConfig;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private ThreadPoolExecutor threadPool;



    public ItemVo queryItemVo(Long skuId) {

        ItemVo itemVo =new ItemVo();
        itemVo.setSkuId(skuId);
        //根据skuId查询sku
        CompletableFuture<SkuInfoEntity> skuCompletable = CompletableFuture.supplyAsync(() -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuByskuId(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity == null) {
                return null;
            }
            itemVo.setWeight(skuInfoEntity.getWeight());
            itemVo.setSkuTitle(skuInfoEntity.getSkuTitle());
            itemVo.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());
            itemVo.setPrice(skuInfoEntity.getPrice());
            return skuInfoEntity;
        },threadPool);

        //根据sku中的CategoryId查询分类
        CompletableFuture<Void> CategoryCompletableFuture = skuCompletable.thenAcceptAsync(skuInfoEntity -> {
            Resp<CategoryEntity> categoryEntityResp = pmsClient.queryCatagoryById(skuInfoEntity.getCatalogId());
            CategoryEntity categoryEntity = categoryEntityResp.getData();
            if (categoryEntity != null) {
                itemVo.setCategoryId(categoryEntity.getCatId());
                itemVo.setCategoryName(categoryEntity.getName());
            }
        },threadPool);

        //根据sku中的BrandId查询品牌
        CompletableFuture<Void> BrandCompletableFuture = skuCompletable.thenAcceptAsync(skuInfoEntity -> {
            Resp<BrandEntity> brandEntityResp = pmsClient.queryBrandById(skuInfoEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResp.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getBrandId());
                itemVo.setBrandName(brandEntity.getName());
            }
        },threadPool);

        //根据sku中的spuId查询spu
        CompletableFuture<Void> spuCompletableFuture = skuCompletable.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.querySpuById(skuInfoEntity.getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
            if (spuInfoEntity != null) {
                itemVo.setSpuId(spuInfoEntity.getId());
                itemVo.setSpuName(spuInfoEntity.getSpuName());
            }
        },threadPool);

        CompletableFuture<Void> imageCompletableFuture = CompletableFuture.runAsync(() -> {
            //根据sku来查询图片
            Resp<List<SkuImagesEntity>> imageResp = pmsClient.querySkuImageBySkuId(skuId);
            List<SkuImagesEntity> images = imageResp.getData();
            itemVo.setImages(images);
        },threadPool);

        CompletableFuture<Void> storeCompletableFuture = CompletableFuture.runAsync(() -> {
            //根据skuId查询库存信息
            Resp<List<WareSkuEntity>> wareSkuBySkuId = wmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> storeInfo = wareSkuBySkuId.getData();
            if (!CollectionUtils.isEmpty(storeInfo)) {
                itemVo.setStore(storeInfo.stream().anyMatch(store -> store.getStock() > 0));
            }
        },threadPool);

        CompletableFuture<Void> smsCompletableFuture = CompletableFuture.runAsync(() -> {
            //根据skuId查询营销信息 积分 满减 打折
            Resp<List<ItemSaleVo>> listResp = smsClient.queryItemSaleVoBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = listResp.getData();
            itemVo.setSales(itemSaleVos);
        },threadPool);

        CompletableFuture<Void> descCompletableFuture = skuCompletable.thenAcceptAsync(skuInfoEntity -> {
            //根据sku中的spuId查询描述信息
            Resp<SpuInfoDescEntity> spuInfoDescEntityResp = pmsClient.querySpudescBySpuId(skuInfoEntity.getSpuId());
            SpuInfoDescEntity spuInfoDescData = spuInfoDescEntityResp.getData();
            if (spuInfoDescData != null) {
                String[] spuDesc = spuInfoDescData.getDecript().split(",");
                itemVo.setDesc(Arrays.asList(spuDesc));
            }
        },threadPool);


        CompletableFuture<Void> groupCompletableFuture = skuCompletable.thenAcceptAsync(skuInfoEntity -> {
            //1.根据sku中的categoryId查询分组
            //2.遍历组到中间表查询每个组的规格参数id
            //3.根据spuId和attrId查询规格参数名和值
            Resp<List<ItemGroupVo>> listResp1 = pmsClient.queryItemGroupVoBySpuId(skuInfoEntity.getCatalogId(), skuInfoEntity.getSpuId());
            List<ItemGroupVo> itemGroupVos = listResp1.getData();
            itemVo.setGroupVos(itemGroupVos);
        },threadPool);

        CompletableFuture<Void> AttrCompletableFuture = skuCompletable.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<SkuSaleAttrValueEntity>> listResp2 = pmsClient.queryAttrSaleBySkuIds(skuInfoEntity.getSpuId());
            List<SkuSaleAttrValueEntity> skuSaleAttr = listResp2.getData();
            itemVo.setSaleAttrValues(skuSaleAttr);
            //1.根据sku中的spuId查询skus
            //2.根据skus查询skuIds
            //3.根据skuId查询销售属性
        },threadPool);


        //allof里的子任务不执行完，会一直堵塞
        CompletableFuture.allOf(CategoryCompletableFuture,BrandCompletableFuture,spuCompletableFuture,imageCompletableFuture,
                storeCompletableFuture,smsCompletableFuture,descCompletableFuture,groupCompletableFuture,AttrCompletableFuture).join();
        return itemVo;
    }
}
