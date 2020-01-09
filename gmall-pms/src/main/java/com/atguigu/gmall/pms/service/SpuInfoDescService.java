package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.SpuInfoDescEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * spu信息介绍
 *
 * @author zhangtaolin
 * @email lxf@atguigu.com
 * @date 2019-12-31 11:37:55
 */
public interface SpuInfoDescService extends IService<SpuInfoDescEntity> {

    PageVo queryPage(QueryCondition params);

    void saveSpuDesc(SpuInfoVo spuInfoVo);
}

