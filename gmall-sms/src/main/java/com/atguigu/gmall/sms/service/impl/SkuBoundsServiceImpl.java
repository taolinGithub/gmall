package com.atguigu.gmall.sms.service.impl;

import com.alibaba.druid.sql.ast.expr.SQLCaseExpr;
import com.atguigu.gmall.sms.dao.SkuFullReductionDao;
import com.atguigu.gmall.sms.dao.SkuLadderDao;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SaleVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.sms.dao.SkuBoundsDao;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsDao, SkuBoundsEntity> implements SkuBoundsService {
    @Autowired
    private SkuLadderDao skuLadderDao;

    @Autowired
    private SkuFullReductionDao reductionDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SkuBoundsEntity> page = this.page(
                new Query<SkuBoundsEntity>().getPage(params),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageVo(page);
    }

    @Transactional
    @Override
    public void saveSales(SaleVo saleVo) {
        //3.营销相关信息
        //3.1skuBounds（积分信息）
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(saleVo,skuBoundsEntity);
        List<String> works = saleVo.getWork();
        skuBoundsEntity.setWork(new Integer(works.get(0))+new Integer(works.get(1))*2+new Integer(works.get(2))*4+new Integer(works.get(3))*8);
        this.save(skuBoundsEntity);
        //3.2skuLadder（打折信息）
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(saleVo,skuLadderEntity);
        skuLadderEntity.setAddOther(saleVo.getLadderAddOther());
        skuLadderDao.insert(skuLadderEntity);
        //3.3FullReduction（满减信息）
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(saleVo,skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(saleVo.getFullAddOther());
        reductionDao.insert(skuFullReductionEntity);

    }

    @Override
    public List<ItemSaleVo> queryItemSaleVoBySkuId(Long skuId) {

        List<ItemSaleVo> itemSaleVos =new ArrayList<>();

        //根据skuId查询积分
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if(skuBoundsEntity!=null){
            ItemSaleVo itemSaleVo =new ItemSaleVo();
            itemSaleVo.setType("积分");
            itemSaleVo.setDesc("赠送"+skuBoundsEntity.getGrowBounds()+"成长积分,赠送"+skuBoundsEntity.getBuyBounds()+"购物积分");
            itemSaleVos.add(itemSaleVo);
        }
        //根据skuId查询打折
        SkuLadderEntity skuLadderEntity = skuLadderDao.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if(skuLadderEntity!=null){
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setType("打折");
            itemSaleVo.setDesc("满"+skuLadderEntity.getFullCount()+"件，打"+skuLadderEntity.getDiscount().divide(new BigDecimal(10))+"折");
            itemSaleVos.add(itemSaleVo);
        }
        //根据skuId查询满减信息

        SkuFullReductionEntity skuFullReductionEntity = reductionDao.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));

        if(skuFullReductionEntity!=null){
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setType("满减");
            itemSaleVo.setDesc("满"+skuFullReductionEntity.getFullPrice()+"元，减"+skuFullReductionEntity.getReducePrice()+"元");
            itemSaleVos.add(itemSaleVo);
        }


        return itemSaleVos;
    }

}