package org.sc.redis.utils;

import java.util.concurrent.TimeUnit;

public interface Lock {

    /**
     * 尝试获取锁
     *
     * @param lockKey  锁的key
     * @param timeout  锁的超时时间
     * @param timeUnit 时间范围
     * @return 成功返回true 否则返回false
     */
    boolean tryLock(String lockKey, long timeout, TimeUnit timeUnit);

    /**
     * 尝试获取锁
     * 使用默认的时间单位(秒)
     *
     * @param lockKey 锁的key
     * @return 成功返回true 否则返回false
     */
    boolean tryLock(String lockKey, long timeout);

    /**
     * 尝试获取锁
     * 使用默认的超时时间
     *
     * @param lockKey 锁的key
     * @return 成功返回true 否则返回false
     */
    boolean tryLock(String lockKey);

    /**
     * 释放锁
     *
     * @param lockKey 锁的key
     */
    void unlock(String lockKey);

}
