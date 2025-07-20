package org.sc.redis.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {
    private String host;

    private int port;

    private String password;

    private int timeout = 5000;

    private int database = 0;

    // 连接池配置
    private Pool pool = new Pool();

    @Data
    public static class Pool {
        private int maxActive = 50;

        private int maxIdle = 20;

        private int minIdle = 2;

        private long maxWait = -1;
    }

}
