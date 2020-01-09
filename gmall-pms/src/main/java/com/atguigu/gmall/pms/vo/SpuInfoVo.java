package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import lombok.Data;

import java.util.List;

@Data
public class SpuInfoVo extends SpuInfoEntity {

    private List<String> spuImages;//spu描述信息（图片）

    private List<BaseAttrVo> baseAttrs; //通用的规格参数

    private List<SkuInfoVo> skus;
}
