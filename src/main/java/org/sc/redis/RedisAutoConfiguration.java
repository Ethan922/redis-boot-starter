package org.sc.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedisClient.class)
    public RedisClient redisClient(StringRedisTemplate redisTemplate) {
        return new RedisClient(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(RedisIdWorker.class)
    public RedisIdWorker idWorker(StringRedisTemplate redisTemplate) {
        return new RedisIdWorker(redisTemplate);
    }

}

