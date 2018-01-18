package com.zhuo.framework;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by liuchen on 18/1/18.
 */
public class RedisCacheTest implements Serializable{

    private static RedisCache redisCache = null;

    @Before
    public void initJedisPool(){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(1);
        jedisPoolConfig.setMaxIdle(1);
        jedisPoolConfig.setMaxWaitMillis(5000);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);
        JedisPool jedisPool = new JedisPool(jedisPoolConfig,"127.0.0.1",6379);
        redisCache = new RedisCache(jedisPool);
    }

    @Test
    public void TestSimple(){
       Object[] objects = new Object[5];
       objects[0] = new Integer(1);
       objects[1] = 2;
       objects[2] = new Double(1.123123123d);
       objects[3] = "test";
       objects[4] = new Byte("100");
       for(int i = 0;i< objects.length;i++){
           redisCache.addToCache(String.valueOf(i), objects[i], 1000);
           Object result = redisCache.getFromCache(String.valueOf(i), objects[i].getClass());
           System.out.println(result.getClass() + ":" + result );
           Assert.assertNotNull(result);
       }
    }

    @Test
    public void TestConcrete(){
        ArrayList arrayList = new ArrayList(){{
            add("1");
            add("2");
        }};
        redisCache.addToCache("1", arrayList, 123);
        Object result = redisCache.getFromCache("1", arrayList.getClass());
        ArrayList arrayList1 = (ArrayList) result;
        Assert.assertNotNull(arrayList1);
    }

}
