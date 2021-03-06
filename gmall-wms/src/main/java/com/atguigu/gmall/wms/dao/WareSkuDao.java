package com.atguigu.gmall.wms.dao;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存
 * 
 * @author zhangtaolin
 * @email lxf@atguigu.com
 * @date 2019-12-31 13:50:08
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {
	
}
