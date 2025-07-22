local key = KEYS[1] --锁的名称
local threadId = ARGV[1] -- 锁的持有者
local timeout = ARGV[2] -- 锁的超时时间，单位:秒
if redis.call("exists", key) == 1 then
    -- 判断锁是否存在
    -- 锁存在，判断是否是当前锁持有者
    if redis.call("hget", key, threadId) then
        -- 是当前锁持有者，锁持有数量加1
        redis.call("hincrby", key, threadId, 1)
        -- 添加超时时间
        redis.call("expire", key, timeout)
        return 1
    else
        -- 不是当前锁持有者，加锁失败，返回false
        return 0
    end
else
    -- 锁不存在，创建锁，锁持有数量为1
    if redis.call("hsetnx", key, threadId, 1) == 1 then
        -- 添加超时时间
        redis.call("expire", key, timeout)
        return 1
    else
        return 0
    end
end