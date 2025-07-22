package org.sc.redis.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class RedisIdWorker {

    private final StringRedisTemplate redisTemplate;

    /**
     * 起始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1099008000L;

    /**
     * 序列号位数
     */
    private static final int COUNT_BITS = 32;

    /**
     * 默认的日期格式
     */
    private static final String DATE_TIME_FORMAT = "yyyyMMdd";

    public RedisIdWorker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 通过redis生成全局唯一id
     *
     * @param key               业务key
     * @param dateTimeFormatter 日期格式
     * @return
     */
    public long getId(String key, DateTimeFormatter dateTimeFormatter) {
        // 获取当前时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 通过redis生成序列号
        String date = now.format(dateTimeFormatter);
        long count = redisTemplate.opsForValue().increment("count:" + key + ":" + date);
        return (timestamp << COUNT_BITS) | count;
    }

    /**
     * 通过redis生成全局唯一id
     * 使用默认的日期格式: yyyyMMdd
     *
     * @param key 业务key
     * @return
     */
    public long getId(String key) {
        return getId(key, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
    }

}
