package org.sc.redis;

import java.util.concurrent.TimeUnit;

public interface Lock {

    String getName();

    /**
     * 尝试获取锁
     *
     * @param timeout  锁的超时时间
     * @param timeUnit 时间范围
     * @return 成功返回true 否则返回false
     */
    boolean tryLock(long timeout, TimeUnit timeUnit);

    /**
     * 尝试获取锁
     * 使用默认的时间单位(秒)
     *
     * @return 成功返回true 否则返回false
     */
    boolean tryLock(long timeout);

    /**
     * 尝试获取锁
     * 使用默认的超时时间
     *
     * @return 成功返回true 否则返回false
     */
    boolean tryLock();

    /**
     * 释放锁
     */
    void unlock();

}
