-- 原子扣减 SKU 库存
-- KEYS[1]: product:sku:stock:{skuId}
-- ARGV[1]: quantity
-- ARGV[2]: initStock (DB 值，key 不存在时初始化用)

local stockKey = KEYS[1]
local quantity = tonumber(ARGV[1])
local initStock = tonumber(ARGV[2])

local exist = redis.call('EXISTS', stockKey)
if exist == 0 then
    redis.call('SET', stockKey, initStock)
    redis.call('EXPIRE', stockKey, 3600)
end

local stock = tonumber(redis.call('GET', stockKey))
if stock < quantity then
    return -1
end

redis.call('DECRBY', stockKey, quantity)
return 1
