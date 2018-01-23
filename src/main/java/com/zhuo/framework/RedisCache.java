package com.zhuo.framework;

import org.apache.commons.codec.binary.Base64;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by liuchen on 18/1/18.
 */
public class RedisCache {

    private JedisPool jedisPool;

    public RedisCache(JedisPool jedisPool){
        this.jedisPool = jedisPool;
    }

    private static Set<Class<?>> simpleClasses = new HashSet<Class<?>>(){{
        add(Boolean.class);
        add(Short.class);
        add(Integer.class);
        add(Long.class);
        add(Float.class);
        add(Double.class);
        add(String.class);
    }};

    /**
     * 这几种类型是不需要编码的
     * @param clazz
     * @return
     */
    private static boolean isSimpleInstance(Class<?> clazz){
        return simpleClasses.contains(clazz);
    }


    private static Object castToType(String val, Class<?> clazz){
        if(clazz == String.class){
            return val;
        }else if(clazz == Integer.class){
            return Integer.parseInt(val);
        }else if(clazz == Boolean.class){
            return Boolean.parseBoolean(val);
        }else if(clazz == Long.class){
            return Long.parseLong(val);
        }else if(clazz == Double.class){
            return Double.parseDouble(val);
        }else if(clazz == Short.class){
            return Short.parseShort(val);
        }else if(clazz == Float.class){
            return Float.parseFloat(val);
        }
        return val;
    }

    /**
     * 序列化
     * @param object
     * @return
     * @throws IOException
     */
    private static String serializeString(Object object) throws IOException{
        if(isSimpleInstance(object.getClass())){
            return String.valueOf(object);
        }else{
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(bos);
            objectOutputStream.writeObject(object);
            objectOutputStream.close();
            return Base64.encodeBase64String(bos.toByteArray());
        }
    }

    /**
     * 反序列化
     * @param val
     * @param clazz
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static Object deSerializeString(String val,Class<?> clazz) throws IOException, ClassNotFoundException,ClassCastException{
        if(isSimpleInstance(clazz)){
            return castToType(val,clazz);
        }else{
            byte[] data = Base64.decodeBase64(val);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(byteArrayInputStream));
            Object o = objectInputStream.readObject();
            objectInputStream.close();
            return clazz.cast(o);
        }
    }

    /**
     * 添加到缓存
     * @param key
     * @param value
     * @param seconds
     * @throws
     */
    public void addToCache(String key, Object value, Integer seconds){
        try (Jedis jedis = jedisPool.getResource()){
            String serializedStr = serializeString(value);
            if (seconds != null) {
                jedis.setex(key,seconds,serializedStr);
            }else{
                jedis.set(key,serializedStr);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 读缓存
     * @param key
     * @param clazz
     * @return
     */
    public Object getFromCache(String key,Class<?> clazz) {
        try(Jedis jedis = jedisPool.getResource()){
            String serializedStr = jedis.get(key);
            if(serializedStr != null){
                return deSerializeString(serializedStr,clazz);
            }
            return null;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 缓存置于无效
     * @param key
     */
    public void disable(String key){
        try(Jedis jedis = jedisPool.getResource()){
            jedis.del(key);
        }
    }


    public static void main(String[] args){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(1);
        jedisPoolConfig.setMaxIdle(1);
        jedisPoolConfig.setMaxWaitMillis(5000);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);
        JedisPool jedisPool = new JedisPool(jedisPoolConfig,"127.0.0.1",6379);
        RedisCache redisCache = new RedisCache(jedisPool);

        ArrayList arrayList = new ArrayList(){{
            add("1");
            add("2");
        }};
        redisCache.addToCache("1", arrayList, 123);
        Object result = redisCache.getFromCache("1", arrayList.getClass());
        ArrayList arrayList1 = (ArrayList) result;
    }

}
