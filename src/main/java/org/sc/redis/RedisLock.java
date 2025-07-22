package org.sc.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisLock implements Lock {

    private final StringRedisTemplate redisTemplate;

    /**
     * 锁的名称
     */
    private final String name;

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

    public RedisLock(StringRedisTemplate redisTemplate, String name) {
        this.redisTemplate = redisTemplate;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * 尝试获取锁
     *
     * @param name     锁的名称
     * @param timeout  锁的默认过期时间
     * @param timeUnit 时间范围
     * @return 成功返回true 否则返回false
     */
    @Override
    public boolean tryLock(String name, long timeout, TimeUnit timeUnit) {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(name, threadId, timeout, timeUnit);
        return Boolean.TRUE.equals(success);
    }


    /**
     * 尝试获取锁
     * 使用默认的时间单位(秒)
     *
     * @param name 锁的名称
     * @return 成功返回true 否则返回false
     */
    @Override
    public boolean tryLock(String name, long timeout) {
        return tryLock(name, timeout, DEFAULT_TIME_UNIT);
    }

    /**
     * 尝试获取锁
     * 使用默认的过期时间(20s)
     *
     * @param name 锁的名称
     * @return 成功返回true 否则返回false
     */
    @Override
    public boolean tryLock(String name) {
        return tryLock(name, DEFAULT_LOCK_TTL, DEFAULT_TIME_UNIT);
    }

    /**
     * 释放锁
     *
     * @param name 锁的名称
     */
    @Override
    public void unlock(String name) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(name), ID_PREFIX + Thread.currentThread().getId());
    }
}
