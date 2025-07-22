local key = KEYS[1] --锁的名称
local threadId = ARGV[1] -- 锁的持有者
-- 判断是否是锁持有者
local holdCount = redis.call("hget", key, threadId)
if holdCount then
    -- 是锁持有者，锁持有数量减1
    if tonumber(holdCount) > 0 then
        -- hincrby在key或field不存在是会自动创建
        redis.call("hincrby", key, threadId, -1)
    end
    -- 判断持有数量是否小于等于0，如果是删除锁
    holdCount = redis.call("hget", key, threadId)
    if tonumber(holdCount) <= 0 then
        redis.call("del", key)
    end
    return 1
else
    return 0
end