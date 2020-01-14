package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam {

    private String key;//搜索关键字

    private  Long[] catelog3;//三级分类id集合

    private Long[] brand;//品牌id集合

    private Double priceFrom;//价格区间开始
    private Double priceTo;//价格区间结束

    private List<String> props;//检索的属性组合

    private String order;//排序规则

    private Integer pageNum=1;//不传默认为1
    private Integer pageSize;//每页大小

    //是否有货
    private Boolean store;

}
