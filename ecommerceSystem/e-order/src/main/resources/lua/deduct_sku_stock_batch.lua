-- 批量原子扣减 SKU 库存，all-or-nothing
-- KEYS: [skuStockKey1, ..., skuStockKeyN]
-- ARGV: [qty1, initStock1, qty2, initStock2, ..., qtyN, initStockN]
-- 返回: 1 成功, -i 表示第 i 个 SKU 库存不足（1-based），其他全部回滚

local n = #KEYS

-- Phase 1: 全部校验通过才扣减
for i = 1, n do
    local key = KEYS[i]
    local qty = tonumber(ARGV[i * 2 - 1])
    local initStock = tonumber(ARGV[i * 2])

    local exist = redis.call('EXISTS', key)
    local stock
    if exist == 0 then
        stock = initStock
    else
        stock = tonumber(redis.call('GET', key))
    end

    if stock < qty then
        return -i
    end
end

-- Phase 2: 全部扣减
for i = 1, n do
    local key = KEYS[i]
    local qty = tonumber(ARGV[i * 2 - 1])
    local initStock = tonumber(ARGV[i * 2])

    local exist = redis.call('EXISTS', key)
    if exist == 0 then
        redis.call('SET', key, initStock - qty)
        redis.call('EXPIRE', key, 3600)
    else
        redis.call('DECRBY', key, qty)
    end
end

return 1
