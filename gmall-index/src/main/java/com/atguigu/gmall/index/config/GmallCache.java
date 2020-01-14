package com.atguigu.gmall.index.config;

import javax.xml.bind.annotation.XmlType;
import java.lang.annotation.*;

@Target(ElementType.METHOD)//作用于方法上
@Retention(RetentionPolicy.RUNTIME)//运行时注解
@Documented
public @interface GmallCache {

    //注解本身没有功能，通过aop赋予功能

    //自定义缓存的key的前缀
    String value() default "";

    //自定义缓存的有效时间
    //单位是分钟
    int timeout() default 30;

    //设置随机值范围，防止雪崩
    int bound() default 5;

    //分布式锁的名称
    String lockName() default "lock";
}
