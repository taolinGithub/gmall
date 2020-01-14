package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /*环绕通知@around
    1.返回值必须是object
    2.形参是proceedingJoinPoint
    3.方法必须抛出throwable异常
    4.joinpoint.proceeding（args）
    * */

    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{


        MethodSignature signature =(MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);

        Class returnType = signature.getReturnType();
        String prefix = gmallCache.value();
        List<Object> args = Arrays.asList(joinPoint.getArgs());
        //1.获取缓存中的数据
        String key =prefix + args;
        String jsonString = redisTemplate.opsForValue().get(key);

        //2.判断数据是否为空
        if(StringUtils.isNotBlank(jsonString)){
             return JSON.parseObject(jsonString,returnType);
        }
        //为空，加分布式锁
        String lockName = gmallCache.lockName();
        RLock lock = redissonClient.getFairLock(lockName+ args);
        lock.lock();

        //4.判断缓存是否为空
        String jsonString1 = redisTemplate.opsForValue().get(key);

        if(StringUtils.isNotBlank(jsonString1)){
            lock.unlock();
            return JSON.parseObject(jsonString1,returnType);
        }

        Object result = joinPoint.proceed(joinPoint.getArgs());//执行目标方法

        //5.把数据放入缓存
        redisTemplate.opsForValue().set(key,JSON.toJSONString(result),gmallCache.timeout()+new Random().nextInt(gmallCache.bound()));

        //6.释放分布式锁
        lock.unlock();
        return result;
    }
}
