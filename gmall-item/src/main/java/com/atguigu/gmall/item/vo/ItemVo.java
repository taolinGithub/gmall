package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;

import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemVo {

    private Long skuId;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private Long spuId;
    private String spuName;

    private String skuTitle;
    private String skuSubTitle;
    private BigDecimal price;
    private BigDecimal weight;
    private Boolean store; //是否有库存

    private List<SkuImagesEntity> images;

    private List<ItemSaleVo> sales;//促销信息

    private List<SkuSaleAttrValueEntity> saleAttrValues; //spu下所有sku的销售组合

    private List<String> desc;

    private List<ItemGroupVo> groupVos;

}
