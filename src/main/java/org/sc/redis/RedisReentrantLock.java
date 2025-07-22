package org.sc.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis实现的可重入分布式锁
 */
public class RedisReentrantLock implements RLock {

    private final StringRedisTemplate redisTemplate;

    /**
     * 锁的名称
     */
    private final String name;

    /**
     * 锁的超时时间
     */
    private long timeout = DEFAULT_LOCK_TTL;

    /**
     * 锁的时间单位
     */
    private TimeUnit timeUnit = DEFAULT_TIME_UNIT;

    /**
     * 锁的默认超时时间：20
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

    /**
     * 获取锁的lua脚本
     */
    private static final DefaultRedisScript<Long> LOCK_SCRIPT;

    // 初始化lua脚本
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.TYPE);

        LOCK_SCRIPT = new DefaultRedisScript<>();
        LOCK_SCRIPT.setLocation(new ClassPathResource("lock.lua"));
        LOCK_SCRIPT.setResultType(Long.TYPE);
    }

    public RedisReentrantLock(StringRedisTemplate redisTemplate, String name) {
        this.redisTemplate = redisTemplate;
        this.name = name;
    }

    /**
     * 获取锁的持有数量
     *
     * @return 锁的持有数量
     */
    @Override
    public long getHoldCount() {
        String holdCount = (String) redisTemplate.opsForHash().get(name, ID_PREFIX + Thread.currentThread().getId());
        return StringUtils.isEmpty(holdCount) ? 0L : Long.parseLong(holdCount);
    }

    /**
     * 获取锁的名称
     *
     * @return 锁的名称
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 尝试获取锁
     *
     * @param timeout  锁的超时时间
     * @param timeUnit 时间单位
     * @return 成功返回true 否则返回false
     */
    @Override
    public boolean tryLock(long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Long success = redisTemplate.execute(
                LOCK_SCRIPT,
                Collections.singletonList(name),
                threadId,
                String.valueOf(timeUnit.toSeconds(timeout))
        );
        return Long.valueOf(1).equals(success);
    }


    /**
     * 尝试获取锁
     * 使用默认的时间单位(秒)
     *
     * @return 成功返回true 否则返回false
     */
    @Override
    public boolean tryLock(long timeout) {
        return tryLock(timeout, DEFAULT_TIME_UNIT);
    }

    /**
     * 尝试获取锁
     * 使用默认的过期时间(20s)
     *
     * @return 成功返回true 否则返回false
     */
    @Override
    public boolean tryLock() {
        return tryLock(DEFAULT_LOCK_TTL, DEFAULT_TIME_UNIT);
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        redisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(name),
                ID_PREFIX + Thread.currentThread().getId(),
                String.valueOf(timeUnit.toSeconds(timeout))
        );
    }
}
