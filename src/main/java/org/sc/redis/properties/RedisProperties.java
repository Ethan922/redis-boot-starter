package org.sc.redis.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {
    private String host;

    private int port;

    private String password;

    private int timeout = 5000;

    private int database = 0;

    // 用于接收 lettuce.pool 相关配置
    private Lettuce lettuce = new Lettuce();

    @Data
    public static class Lettuce {
        private Pool pool = new Pool();

        @Data
        public static class Pool {
            // 连接池最大连接数
            private int maxActive = 8;
            // 连接池最大阻塞等待时间
            private Duration maxWait = Duration.ofMillis(-1);
            // 连接池中的最大空闲连接数
            private int maxIdle = 8;
            // 连接池中的最小空闲连接数
            private int minIdle = 0;
        }
    }

}
