package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.*;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.BaseAttrVo;
import com.atguigu.gmall.pms.vo.SkuInfoVo;
import com.atguigu.gmall.pms.vo.SpuInfoVo;
import com.atguigu.gmall.sms.vo.SaleVo;

import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescDao descDao;
    @Autowired
    private ProductAttrValueService attrValueService;
    @Autowired
    private SkuInfoDao skuInfoDao;
    @Autowired
    private SkuImagesService imagesService;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Autowired
    private SpuInfoDescService saveSpuDesc;


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo querySpuInfo(QueryCondition queryCondition, Long catId) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        if (catId != 0) {
            wrapper.eq("catalog_id", catId);
        }
        String key = queryCondition.getKey();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(t -> t.like("spu_name", key).or().eq("id", key));
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(queryCondition),
                wrapper
        );

        return new PageVo(page);


    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuInfoVo spuInfoVo) {
        //保存spu相关信息
        //1.1spuinfo
        this.saveSpuInfo(spuInfoVo);

        //1.2spuInfoDesc spu描述信息
        this.saveSpuDesc.saveSpuDesc(spuInfoVo);
        //1.3基础属性相关信息（pms_product_attr_value）

        this.saveBaseAttrs(spuInfoVo);


        //2.sku相关信息
        this.saveSkuInfoWithSaleInfo(spuInfoVo);


    }

    public void saveSkuInfoWithSaleInfo(SpuInfoVo spuInfoVo) {
        List<SkuInfoVo> skus = spuInfoVo.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }

        skus.forEach(sku -> {
            //2.1skuinfo
            SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
            BeanUtils.copyProperties(sku, skuInfoEntity);
            skuInfoEntity.setSpuId(spuInfoVo.getId());
            List<String> images = sku.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                skuInfoEntity.setSkuDefaultImg(skuInfoEntity.getSkuDefaultImg() == null ? images.get(0) : skuInfoEntity.getSkuDefaultImg());
            }
            skuInfoEntity.setSkuCode(UUID.randomUUID().toString());
            skuInfoEntity.setCatalogId(spuInfoVo.getCatalogId());
            skuInfoEntity.setBrandId(spuInfoVo.getBrandId());
            this.skuInfoDao.insert(skuInfoEntity);
            Long skuId = skuInfoEntity.getSkuId();

            //2.2skuinfoImages
            if (!CollectionUtils.isEmpty(images)) {
                List<SkuImagesEntity> imagesEntities = images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(image);
                    skuImagesEntity.setImgSort(0);
                    skuImagesEntity.setDefaultImg(StringUtils.equals(image, skuInfoEntity.getSkuDefaultImg()) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList());

                this.imagesService.saveBatch(imagesEntities);
            }

            //2.3skusaleAttrValue（销售属性）
            List<SkuSaleAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)) {
                saleAttrs.forEach(skuSaleAttrValueEntity -> {
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    skuSaleAttrValueEntity.setAttrSort(0);
                });
                skuSaleAttrValueService.saveBatch(saleAttrs);
            }
            //3.营销相关信息
            SaleVo saleVo = new SaleVo();
            BeanUtils.copyProperties(sku, saleVo);
            saleVo.setSkuId(skuId);
            gmallSmsClient.saveSales(saleVo);

        });
    }

    public void saveBaseAttrs(SpuInfoVo spuInfoVo) {
        List<BaseAttrVo> baseAttrs = spuInfoVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<ProductAttrValueEntity> attrValues = baseAttrs.stream().map(baseAttrVo -> {
                ProductAttrValueEntity attrValueEntity = new ProductAttrValueEntity();
                BeanUtils.copyProperties(baseAttrVo, attrValueEntity);
                attrValueEntity.setSpuId(spuInfoVo.getId());
                attrValueEntity.setAttrSort(0);
                attrValueEntity.setQuickShow(0);
                return attrValueEntity;
            }).collect(Collectors.toList());
            this.attrValueService.saveBatch(attrValues);
        }
    }

    @Transactional
    public void saveSpuInfo(SpuInfoVo spuInfoVo) {
        this.save(spuInfoVo);
        spuInfoVo.setCreateTime(new Date());
        spuInfoVo.setUodateTime(spuInfoVo.getCreateTime());

    }

}