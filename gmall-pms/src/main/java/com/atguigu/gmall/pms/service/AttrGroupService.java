package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.GroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 属性分组
 *
 * @author zhangtaolin
 * @email lxf@atguigu.com
 * @date 2019-12-31 11:37:55
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageVo queryPage(QueryCondition params);

    PageVo queryGroupByCidPage(QueryCondition queryCondition, long catId);

    GroupVo queryGroupVoByGid(Long gid);

    List<GroupVo> queryGroupVoByCid(Long catId);
}

