package org.sc.redis;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class RedisClient {

    // 默认过期时间(30)
    private static final long DEFAULT_TTL = 30L;

    // 锁的默认过期时间(10)
    private static final long DEFAULT_LOCK_TTL = 20L;

    // 默认的时间单位(秒)
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    private final StringRedisTemplate redisTemplate;

    public RedisClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class RedisData {
        private Object data;
        private LocalDateTime expireTime;
    }

    /**
     * 设置缓存以及过期时间
     *
     * @param key      key
     * @param value    value
     * @param time     过期时间
     * @param timeUnit 时间范围
     */
    public void set(String key, Object value, long time, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }

    /**
     * 设置无过期时间的缓存
     *
     * @param key   key
     * @param value key
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value));
    }

    /**
     * 以默认时间设置缓存，默认逻辑过期时间为30秒
     *
     * @param key   key
     * @param value value
     */
    public void setWithDefaultExpireTime(String key, Object value) {
        this.set(key, value, DEFAULT_TTL, DEFAULT_TIME_UNIT);
    }

    /**
     * 设置逻辑过期的缓存
     *
     * @param key      key
     * @param value    value
     * @param time     逻辑过期时间
     * @param timeUnit 时间单位
     */
    public void setWithLogicalExpire(String key, Object value, long time, TimeUnit timeUnit) {
        RedisData redisData = new RedisData(value, LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        this.set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 设置逻辑过期的缓存，默认过期时间为30秒
     *
     * @param key   key
     * @param value value
     */
    public void setWithLogicalExpire(String key, Object value) {
        RedisData redisData = new RedisData(value, LocalDateTime.now().plusSeconds(DEFAULT_TIME_UNIT.toSeconds(DEFAULT_TTL)));
        this.set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 查询缓存
     *
     * @param key   key
     * @param clazz 返回结果类型的字节码对象
     * @param <R>   返回结果类型
     * @return 查询结果
     */
    public <R> R get(String key, Class<R> clazz) {
        String jsonStr = redisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        return JSONUtil.toBean(jsonStr, clazz);
    }

    /**
     * 针对缓存穿透问题的查询，数据库查询不到时缓存空值
     * 使用互斥锁控制只有一个线程查询数据库
     *
     * @param key          key
     * @param clazz        返回结果类型的字节码对象
     * @param dbQuery      数据库查询函数
     * @param param        查询函数的参数
     * @param time         过期时间
     * @param timeUnit     时间单位
     * @param lockName      锁的名称
     * @param lockTime     锁的过期时间
     * @param lockTimeUnit 锁的过期时间单位
     * @param <R>          返回结果类型
     * @param <T>          查询函数的参数类型
     * @return
     */
    public <R, T> R queryWithPenetrationAndMutex(String key, Class<R> clazz, Function<T, R> dbQuery, T param,
                                                 long time, TimeUnit timeUnit, String lockName, long lockTime, TimeUnit lockTimeUnit) {
        String jsonStr = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(jsonStr)) {
            return JSONUtil.toBean(jsonStr, clazz);
        }
        // jsonStr为空字符串
        if (jsonStr != null) {
            return null;
        }
        Lock lock = getLock(lockName);
        // 锁自旋
        while (!lock.tryLock(lockName, lockTime, lockTimeUnit)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return queryWithPenetration(key, clazz, dbQuery, param, time, timeUnit);
        } finally {
            lock.unlock(lockName);
        }
    }

    /**
     * 针对缓存穿透问题的查询，数据库查询不到时缓存空值
     * 不使用互斥锁
     *
     * @param key      key
     * @param clazz    返回结果类型的字节码对象
     * @param dbQuery  数据库查询函数
     * @param param    查询函数的参数
     * @param time     过期时间
     * @param timeUnit 时间单位
     * @param <R>      返回结果类型
     * @param <T>      查询函数的参数类型
     * @return
     */
    public <R, T> R queryWithPenetration(String key, Class<R> clazz, Function<T, R> dbQuery, T param, long time, TimeUnit timeUnit) {
        String jsonStr = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(jsonStr)) {
            return JSONUtil.toBean(jsonStr, clazz);
        }
        // jsonStr为空字符串
        if (jsonStr != null) {
            return null;
        }
        R r = dbQuery.apply(param);
        if (r == null) {
            this.set(key, StrUtil.EMPTY, time, timeUnit);
            return null;
        }
        this.set(key, r, time, timeUnit);
        return r;
    }

    /**
     * 针对缓存穿透问题的查询，数据库查询不到时缓存空值
     * 不使用互斥锁
     * 设置默认的过期时间，默认过期时间为30秒
     *
     * @param key     key
     * @param clazz   返回结果类型的字节码对象
     * @param dbQuery 数据库查询函数
     * @param param   查询函数的参数
     * @param <R>     返回结果类型
     * @param <T>     查询函数的参数类型
     * @return
     */
    public <R, T> R queryWithPenetration(String key, Class<R> clazz, Function<T, R> dbQuery, T param) {
        return this.queryWithPenetration(key, clazz, dbQuery, param, DEFAULT_TTL, DEFAULT_TIME_UNIT);
    }

    /**
     * 针对缓存穿透问题的查询，数据库查询不到时缓存空值
     * 使用互斥锁控制只有一个线程查询数据库
     * 设置默认的过期时间，默认过期时间为30秒
     *
     * @param key          key
     * @param clazz        返回结果类型的字节码对象
     * @param dbQuery      数据库查询函数
     * @param param        查询函数的参数
     * @param lockName      锁的名称
     * @param lockTime     锁的过期时间
     * @param lockTimeUnit 锁的过期时间单位
     * @param <R>          返回结果类型
     * @param <T>          查询函数的参数类型
     * @return
     */
    public <R, T> R queryWithPenetrationAndMutex(String key, Class<R> clazz, Function<T, R> dbQuery, T param,
                                                 String lockName, long lockTime, TimeUnit lockTimeUnit) {
        return this.queryWithPenetrationAndMutex(key, clazz, dbQuery, param, DEFAULT_TTL, DEFAULT_TIME_UNIT, lockName, lockTime, lockTimeUnit);
    }

    /**
     * 针对缓存穿透问题的查询，数据库查询不到时缓存空值
     * 使用互斥锁控制只有一个线程查询数据库
     * 设置锁的默认过期时间，默认过期时间为30秒
     *
     * @param key     key
     * @param clazz   返回结果类型的字节码对象
     * @param dbQuery 数据库查询函数
     * @param param   查询函数的参数
     * @param lockName 锁的名称
     * @param <R>     返回结果类型
     * @param <T>     查询函数的参数类型
     * @return
     */
    public <R, T> R queryWithPenetrationAndMutex(String key, Class<R> clazz, Function<T, R> dbQuery, T param,
                                                 long time, TimeUnit timeUnit, String lockName) {
        return this.queryWithPenetrationAndMutex(key, clazz, dbQuery, param, time, timeUnit, lockName, DEFAULT_LOCK_TTL, DEFAULT_TIME_UNIT);
    }

    /**
     * 使用逻辑过期的方式解决缓存击穿问题
     * 需要缓存预热，可能会造成短暂的数据不一致情况
     *
     * @param key          数据的key
     * @param clazz        返回结果类型的字节码对象
     * @param dbQuery      数据库查询函数
     * @param param        查询函数的参数
     * @param time         数据过期时间
     * @param timeUnit     数据的过期时间单位
     * @param lockName      锁的名称
     * @param lockTime     锁的过期时间
     * @param lockTimeUnit 锁的过期时间单位
     * @param <R>          返回结果类型
     * @param <T>          查询函数的参数类型
     * @return
     */
    public <R, T> R queryWithLogicalExpire(String key, Class<R> clazz, Function<T, R> dbQuery, T param,
                                           long time, TimeUnit timeUnit, String lockName, long lockTime, TimeUnit lockTimeUnit) {
        String jsonStr = redisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(jsonStr, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), clazz);
        // 判断是否过期
        if (LocalDateTime.now().isBefore(redisData.getExpireTime())) {
            return r;
        }
        // 已过期，需要更新缓存
        // 获取锁
        Lock lock = getLock(lockName);
        if (!lock.tryLock(lockName, lockTime, lockTimeUnit)) {
            // 获取锁失败，有其他线程在更新缓存，返回旧数据
            return r;
        }
        // 异步更新缓存
        ThreadUtil.execAsync(() -> {
            try {
                // 从数据库中查询数据
                R newR = dbQuery.apply(param);
                this.setWithLogicalExpire(key, newR, time, timeUnit);
            } finally {
                lock.unlock(lockName);
            }
        });
        return r;
    }

    /**
     * 使用逻辑过期的方式解决缓存击穿问题
     * 需要缓存预热，可能会造成短暂的数据不一致情况
     * 锁使用默认的过期时间，默认的过期时间为30秒
     *
     * @param key      数据的key
     * @param clazz    返回结果类型的字节码对象
     * @param dbQuery  数据库查询函数
     * @param param    查询函数的参数
     * @param time     数据过期时间
     * @param timeUnit 数据的过期时间单位
     * @param lockName  锁的名称
     * @param <R>      返回结果类型
     * @param <T>      查询函数的参数类型
     * @return
     */
    public <R, T> R queryWithLogicalExpire(String key, Class<R> clazz, Function<T, R> dbQuery, T param,
                                           long time, TimeUnit timeUnit, String lockName) {
        return this.queryWithLogicalExpire(key, clazz, dbQuery, param, time, timeUnit, lockName, DEFAULT_LOCK_TTL, DEFAULT_TIME_UNIT);
    }

    public Lock getLock(String name) {
        return new RedisLock(redisTemplate, name);
    }
}
