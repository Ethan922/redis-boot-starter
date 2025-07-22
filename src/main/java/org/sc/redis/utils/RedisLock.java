package org.sc.redis.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisLock implements Lock {

    private final StringRedisTemplate redisTemplate;

    /**
     * 锁的默认过期时间：20
     */
    private static final long DEFAULT_LOCK_TTL = 20L;

    /**
     * 默认时间单位
     */
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    /**
     * 锁持有者的标识前缀
     */
    private static final String ID_PREFIX = UUID.randomUUID().toString().replace("-", "") + "-";

    /**
     * 释放锁的lua脚本
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    // 初始化lua脚本
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.TYPE);
    }

    public RedisLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey  锁的key
     * @param timeout  锁的默认过期时间
     * @param timeUnit 时间范围
     * @return 成功返回true 否则返回false
     */
    @Override
    public boolean tryLock(String lockKey, long timeout, TimeUnit timeUnit) {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, threadId, timeout, timeUnit);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 尝试获取锁
     * 使用默认的过期时间(20s)
     *
     * @param lockKey 锁的key
     * @return 成功返回true 否则返回false
     */
    @Override
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_LOCK_TTL, DEFAULT_TIME_UNIT);
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的key
     */
    @Override
    public void unlock(String lockKey) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), ID_PREFIX + Thread.currentThread().getId());
    }
}
