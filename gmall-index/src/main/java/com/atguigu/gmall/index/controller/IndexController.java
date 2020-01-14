package com.atguigu.gmall.index.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("index")
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping("cates")
    public Resp<List<CategoryEntity>> queryLev1ById(){
        List<CategoryEntity> categoryEntities=indexService.queryLev1ById();

        return Resp.ok(categoryEntities);
    }

    @GetMapping("cates/{pid}")
    public Resp<List<CategoryVo>> queryCatagory(@PathVariable("pid") Long pid){
        List<CategoryVo> categoryVos=indexService.queryCatagory(pid);
        return Resp.ok(categoryVos);
    }

    @GetMapping("testlock")
    public Resp<Object> testLock(){
        indexService.testLock();
        return Resp.ok(null);
    }

    @GetMapping("read")
    public Resp<String> read(){
        String msg = indexService.readLock();

        return Resp.ok(msg);
    }

    @GetMapping("write")
    public Resp<String> write(){
        String msg = indexService.writeLock();

        return Resp.ok(msg);
    }

    @GetMapping("lanch")
    public Resp<String> lanch(){
        String msg = indexService.lanch();

        return Resp.ok(msg);
    }

    @GetMapping("coutdown")
    public Resp<String> coutdown(){
        String msg = indexService.coutdown();

        return Resp.ok(msg);
    }

}
