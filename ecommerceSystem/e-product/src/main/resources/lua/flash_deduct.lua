-- ============================================================
-- KEYS[1] = flash:stock:{flashSaleId}      ← 秒杀库存 key（String，预热时写入）
-- KEYS[2] = flash:user:{flashSaleId}       ← 用户购买计数 key（Hash，field=userId）
-- ARGV[1] = userId
-- ARGV[2] = quantity
-- ARGV[3] = perUserLimit（每人限购数量）
-- ARGV[4] = ttlSeconds（key 过期时间，场次结束后保留 1 小时）
-- 返回值:  1 = 扣减成功  /  0 = 库存不足  /  -1 = 超出限购
-- ============================================================

-- 1. 查询当前用户在该秒杀活动中已购买的数量
local bought = redis.call('HGET', KEYS[2], ARGV[1])       -- Hash 取值，字段=userId，若不存在返回 false
bought = bought and tonumber(bought) or 0                  -- 转为数字，nil/false 时默认 0

-- 2. 校验是否超出每人限购数量
if bought + tonumber(ARGV[2]) > tonumber(ARGV[3]) then     -- 已购数量 + 本次购买数量 > 限购上限？
    return -1                                              -- 超出限购，直接拒绝
end

-- 3. 原子扣减秒杀库存（DECRBY 是原子操作，无需提前 GET 判断，避免并发窗口）
local stock = redis.call('DECRBY', KEYS[1], ARGV[2])      -- 库存减去购买数量，返回扣减后的值
if stock < 0 then                                           -- 库存不够、已被抢光
    redis.call('INCRBY', KEYS[1], ARGV[2])                  -- 立刻回滚库存（加回去）
    return 0                                                -- 返回库存不足
end

-- 4. 累加用户已购数量（HINCRBY 对 Hash 中不存在的 field 会自动初始化为 0 再加）
redis.call('HINCRBY', KEYS[2], ARGV[1], ARGV[2])           -- Hash field=userId 的值 +quantity

-- 5. 设置过期时间（首次创建 key 时生效，后续调用刷新 TTL，防止 key 永不过期）
redis.call('EXPIRE', KEYS[1], ARGV[4])                       -- 延期/设置库存 key 过期时间
redis.call('EXPIRE', KEYS[2], ARGV[4])                       -- 延期/设置用户购买记录 key 过期时间
return 1                                                    -- 秒杀成功
