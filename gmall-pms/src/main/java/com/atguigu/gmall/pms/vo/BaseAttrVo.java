package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;


import java.util.List;


public class BaseAttrVo extends ProductAttrValueEntity {


    public void setValueSelected(List<String> valueSelected) {

        if (!CollectionUtils.isEmpty(valueSelected)) {
            this.setAttrValue(StringUtils.join(valueSelected, ","));
        } else {
            this.setAttrValue(null);
        }

    }
}
