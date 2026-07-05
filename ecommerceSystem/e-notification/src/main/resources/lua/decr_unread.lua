-- 原子执行 DECR：若结果小于等于 0 则删除 key（防止出现负数），否则返回当前值
local val = redis.call('DECR', KEYS[1])
if val <= 0 then
    redis.call('DEL', KEYS[1])
    return 0
end
return val
