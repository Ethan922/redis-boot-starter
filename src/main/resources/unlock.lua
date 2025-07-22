local key = KEYS[1] --锁的名称
local threadId = ARGV[1] -- 锁的持有者
local timeout = ARGV[2] -- 锁的超时时间
-- 判断是否是锁持有者
if redis.call("hexists", key, threadId) == 0 then
    -- 不是锁持有者
    return 0
end
-- 是锁持有者，锁持有数量减1
local holdCount = redis.call("hincrby", key, threadId, -1)
if holdCount > 0 then
    -- 大于0更新锁的超时时间
    redis.call("expire", key, timeout)
else
    -- 小于等于0，删除锁
    redis.call("del", key)
end
return 1