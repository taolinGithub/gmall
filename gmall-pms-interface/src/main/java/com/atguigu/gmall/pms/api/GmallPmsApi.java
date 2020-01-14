package com.atguigu.gmall.pms.api;

import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {

    @GetMapping("pms/spuinfo/info/{id}")
    public Resp<SpuInfoEntity> querySpuById(@PathVariable("id") Long id);


    @PostMapping("pms/spuinfo/page")
    public Resp<List<SpuInfoEntity>> querySpuBypage(@RequestBody QueryCondition condition);

    @GetMapping("pms/skuinfo/{spuId}")
    public Resp<List<SkuInfoEntity>> querySkuBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/brand/info/{brandId}")
    public Resp<BrandEntity> queryBrandById(@PathVariable("brandId") Long brandId);

    @GetMapping("pms/category/info/{catId}")
    public Resp<CategoryEntity> queryCatagoryById(@PathVariable("catId") Long catId);

    @PostMapping("pms/productattrvalue/{spuId}")
    public Resp<List<ProductAttrValueEntity>> queryAttr(@PathVariable("spuId")Long spuId);

    //查询一级分类
    @GetMapping("pms/category")
    public Resp<List<CategoryEntity>> queryCategoresByLevelOrPid(@RequestParam(value = "level", defaultValue = "0") Integer level, @RequestParam(value = "parentCid", required = false) Long pid);


    @GetMapping("pms/category/{pid}")
    public Resp<List<CategoryVo>> queryCategoryById(@PathVariable("pid") Long pid);


    @GetMapping("pms/skuinfo/info/{skuId}")
    public Resp<SkuInfoEntity> querySkuByskuId(@PathVariable("skuId") Long skuId);

    //根据skuid查询图片
    @GetMapping("pms/skuimages/{skuId}")
    public Resp<List<SkuImagesEntity>> querySkuImageBySkuId(@PathVariable("skuId")Long skuId);



    @GetMapping("pms/spuinfodesc/info/{spuId}")
    public Resp<SpuInfoDescEntity> querySpudescBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/attrgroup/withattr")
    public Resp<List<ItemGroupVo>> queryItemGroupVoBySpuId(@RequestParam("cid")Long cid, @RequestParam("spuId")Long spuId);

    @GetMapping("pms/skusaleattrvalue/{spuId}")
    public Resp<List<SkuSaleAttrValueEntity>> queryAttrSaleBySkuIds(@PathVariable("spuId") Long spuId);



    }
