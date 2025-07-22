# Redis客户端工具集

## 功能概述

封装了一套基于Redis的简单工具类，主要包括以下功能：

### 1. 全局唯一ID生成（RedisIdWorker）
- 使用`时间戳` + `计数器`方式生成全局唯一ID

### 2. 缓存操作（RedisClient）
#### 缓存查询
- 基础缓存查询与设置
- 针对缓存穿透问题的解决方案：
  - **缓存空值**
  - 加**分布式锁**限制数据库查询
- 针对缓存击穿问题的解决方案：
  - **逻辑过期**机制
  - 异步更新缓存

#### 分布式锁
- 基于Redis的分布式锁
- 支持指定锁的超时时间
- 使用lua脚本实现释放锁操作的原子性

## 使用说明
### 1. 引入依赖（pom.xml）
```
<dependency>
     <groupId>org.sc</groupId>
     <artifactId>redis-boot-starter</artifactId>
     <version>1.0.0</version>
</dependency>
```

### 2. 使用示例
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
  
  适用于数据一致性要求不高的场景，可能造成短暂的**数据不一致**。
  
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

- **分布式锁**

  尝试获取锁

  ```
  Lock lock = redisClient.getLock("lockName");
  boolean isLock = lock.tryLock("lockName", 30, TimeUnit.SECONDS) // 获取成功返回true 否则返回false
  ```

  释放锁

  ```
  lock.unlock("lockName");
  ```

  

