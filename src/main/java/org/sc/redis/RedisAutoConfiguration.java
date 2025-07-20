package org.sc.redis;

import org.sc.redis.config.RedisConfig;
import org.sc.redis.properties.RedisProperties;
import org.sc.redis.utils.RedisIdWorker;
import org.sc.redis.utils.RedisClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@Import(RedisConfig.class)
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

