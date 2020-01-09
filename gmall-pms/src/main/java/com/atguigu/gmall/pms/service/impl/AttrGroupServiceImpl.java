package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.AttrAttrgroupRelationDao;
import com.atguigu.gmall.pms.dao.AttrDao;
import com.atguigu.gmall.pms.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.AttrGroupDao;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrAttrgroupRelationDao relationDao;

    @Autowired
    private AttrDao attrDao;


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo queryGroupByCidPage(QueryCondition queryCondition, long catId) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(queryCondition),
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catId)
        );

        return new PageVo(page);
    }

    @Override
    public GroupVo queryGroupVoByGid(Long gid) {
        GroupVo groupVo = new GroupVo();

        //根据gid查询组
        AttrGroupEntity byId = this.getById(gid);
        BeanUtils.copyProperties(byId, groupVo);
        //查询中间表
        List<AttrAttrgroupRelationEntity> attr_group_id = this.relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", gid));
        groupVo.setRelations(attr_group_id);

        //判断中间表是否为空
        if (CollectionUtils.isEmpty(attr_group_id)) {
            return groupVo;
        }

        //获取所有规格参数的id
        List<Long> attrIds = attr_group_id.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());

        //查询规格参数
        List<AttrEntity> attrEntities = attrDao.selectBatchIds(attrIds);


        groupVo.setAttrEntities(attrEntities);
        return groupVo;
    }

    @Override
    public List<GroupVo> queryGroupVoByCid(Long catId) {

        //根据分类的id查询规格参数组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catId));

        //遍历规格参数组查询每个组下的规格参数
        return groupEntities.stream().map(attrGroupEntity -> queryGroupVoByGid(attrGroupEntity.getAttrGroupId())).collect(Collectors.toList());


    }

}