# Redis客户端工具集

## 功能概述

本项目提供了一套基于Spring Data Redis封装的Redis客户端工具类，简化了Redis在业务场景中的使用，主要包括以下功能：

### 1. 全局唯一ID生成（RedisIdWorker）
- 使用Redis自增和时间戳结合的方式生成全局唯一ID

### 2. 缓存操作（RedisClient）
#### 缓存查询
- 基础缓存查询与设置
- 针对缓存穿透问题的解决方案：
  - 查询数据库并缓存空值
  - 使用Redis实现的互斥锁控制数据库查询
- 针对缓存击穿问题的解决方案：
  - 使用逻辑过期机制
  - 异步更新缓存

#### 分布式锁
- 基于Redis的分布式锁实现
- 支持指定锁的过期时间
- 默认锁过期时间为20秒
- 提供锁的获取与释放接口

## 使用说明
### 1. 引入依赖（pom.xml）
```
<dependency>
     <groupId>org.sc</groupId>
     <artifactId>redis-boot-starter</artifactId>
     <version>1.0.0</version>
</dependency>
```

### 2. 配置Redis连接
在`application.yml`或`application.properties`中配置Redis连接信息

```
spring:
  redis:
    host: localhost
    port: 6379
    password: xxxxxx
    database: 0
    timeout: 5000
    pool:
        # 连接池最大连接数（使用负值表示没有限制）
        max-active: 50
        # 连接池最大阻塞等待时间，单位ms（使用负值表示没有限制）
        max-wait: 3000
        # 连接池中的最大空闲连接数
        max-idle: 20
        # 连接池中的最小空闲连接数
        min-idle: 2
```

### 3. 使用示例
- **生成全局唯一ID**
  ```java
  @Autowired
  private RedisIdWorker redisIdWorker;
  long id = redisIdWorker.getId("businessKey");
  // redis中生成的记录为：count:businessKey:20250720 1
  ```


- **缓存设置与查询**
  
  ```java
  @Autowired
  private RedisClient redisClient;
  // 设置缓存
  redisClient.set("key", "value", 30, TimeUnit.SECONDS);
  
  // 查询缓存
  String value = redisClient.get("key", User.class);
  ```


- **缓存穿透场景下的查询（使用Redis互斥锁）**
  
  ```java
  String result = redisClient.queryWithPenetrationAndMutex(
      "key", 
      User.class, 
      param -> dbQuery(param), 
      param, 
      "lockKey", 
      20, 
      TimeUnit.SECONDS
  );
  ```


- **缓存击穿场景下的查询（采用逻辑过期方案，需要提前缓存预热）**
  
  适用于数据一致性要求不高的场景，可能造成短暂的数据不一致。
  
  ```java
  String result = redisClient.queryWithLogicalExpire(
      "key", 
      User.class, 
      param -> dbQuery(param), 
      param, 
      30, 
      TimeUnit.SECONDS, 
      "lockKey"
  );
  ```
