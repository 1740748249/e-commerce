-- KEYS[1]: coupon:meta:{couponId}  (Hash: stock, status)
-- KEYS[2]: coupon:user:claimed:{couponId}:{userId}
-- ARGV[1]: limitPerUser
-- ARGV[2]: initStock  (Hash 不存在时初始化用)
-- ARGV[3]: initStatus (Hash 不存在时初始化用)

local metaKey = KEYS[1]
local userClaimKey = KEYS[2]
local limitPerUser = tonumber(ARGV[1])
local initStock = tonumber(ARGV[2])
local initStatus = ARGV[3]

-- 首次访问则初始化 Hash（库存 + 状态合一）
local exist = redis.call('EXISTS', metaKey)
if exist == 0 then
    redis.call('HSET', metaKey, 'stock', initStock, 'status', initStatus)
    redis.call('EXPIRE', metaKey, 3600)
end

-- 状态检查（与库存扣减在同一原子操作内，消除 TOCTOU 窗口）
local status = redis.call('HGET', metaKey, 'status')
if status ~= '1' then
    return -3   -- 优惠券已停用
end

local stock = tonumber(redis.call('HGET', metaKey, 'stock'))
if stock <= 0 then
    return -1   -- 库存不足
end

local userCount = tonumber(redis.call('GET', userClaimKey) or '0')
if userCount >= limitPerUser then
    return -2   -- 已达限领上限
end

redis.call('HINCRBY', metaKey, 'stock', -1)
redis.call('INCR', userClaimKey)
redis.call('EXPIRE', userClaimKey, 2592000)
return 1        -- 成功
