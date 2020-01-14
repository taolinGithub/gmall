package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX="index:cates:";

    @Autowired
    private GmallPmsApi gmallPmsApi;

    @Autowired
    private RedissonClient redissonClient;

    public List<CategoryEntity> queryLev1ById() {
        Resp<List<CategoryEntity>> listResp = gmallPmsApi.queryCategoresByLevelOrPid(1, null);
        List<CategoryEntity> categoryEntities = listResp.getData();

        return categoryEntities;
    }

    @GmallCache(value = "index:cates:",timeout = 7200,bound = 100,lockName = "lock")
    public List<CategoryVo> queryCatagory(Long pid) {

//        //查询缓存里
//        String s = stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
//        //如果有，直接用缓存
//        if(!StringUtils.isEmpty(s)){
//            return JSON.parseArray(s,CategoryVo.class);
//        }
//        //加分布式锁 放行一个去查询数据库，剩下的查询缓存,加唯一标志为了提高性能
//        RLock lock = redissonClient.getLock("lock"+pid);
//        lock.lock();
//        //查询缓存里
//        String s1 = stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
//        //如果有，直接用缓存
//        if(!StringUtils.isEmpty(s1)){
//            lock.unlock();
//            return JSON.parseArray(s1,CategoryVo.class);
//        }

        Resp<List<CategoryVo>> listResp = gmallPmsApi.queryCategoryById(pid);
        List<CategoryVo> categoryVos = listResp.getData();

//        //查询完成后设置缓存
//        stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid,JSON.toJSONString(categoryVos),5+ new Random().nextInt(5));
//
//        lock.unlock();

        return categoryVos;
    }
    public synchronized void testLock() {

        RLock lock = redissonClient.getLock("lock");
        lock.lock(10,TimeUnit.SECONDS);//加锁

        // 查询redis中的num值
            String value = this.stringRedisTemplate.opsForValue().get("num");
            // 没有该值return
            if (StringUtils.isBlank(value)){
                return ;
            }
            // 有值就转成成int
            int num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));

         //   lock.unlock();
    }

    //读写锁之读锁
    public String readLock() {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10,TimeUnit.SECONDS);
        String msg = stringRedisTemplate.opsForValue().get("msg");

      // rwLock.readLock().unlock();

        return "读取数据："+msg;
}
    //读写锁之写锁  读读并发，读写，写写不能并发
    public String writeLock() {

        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10,TimeUnit.SECONDS);
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set("msg", uuid);

        //rwLock.writeLock().unlock();
        return "写入数据"+uuid;
    }

    public String lanch() {
        RCountDownLatch lanch = redissonClient.getCountDownLatch("lanch");
        lanch.trySetCount(6);//6名同学
        try {
            lanch.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return "锁门走人";
    }

    public String coutdown() {
        RCountDownLatch lanch = redissonClient.getCountDownLatch("lanch");
        lanch.countDown();

        return "我走了哦";
    }

   /* public synchronized void testLock() {

        //从redis中获取锁
        String uuid = UUID.randomUUID().toString();
        //防止死锁现象，设置过期时间
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid,3,TimeUnit.SECONDS);

        if(lock){
            // 查询redis中的num值
            String value = this.stringRedisTemplate.opsForValue().get("num");
            // 没有该值return
            if (StringUtils.isBlank(value)){
                return ;
            }
            // 有值就转成成int
            int num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));

            //释放锁
//            String lock1 = stringRedisTemplate.opsForValue().get("lock");
                //防止误删锁
//            if(StringUtils.equals(lock1,uuid)){
//                stringRedisTemplate.delete("lock");
//            }
                //lua脚本，保证原子性
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            //执行lua脚本
            this.stringRedisTemplate.execute(new DefaultRedisScript<>(script), Arrays.asList("lock"), Arrays.asList(uuid));
        }
        else{
            try {
                Thread.sleep(1000);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }*/
}
