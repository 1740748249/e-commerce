-- ============================================================
-- 原子补偿：检查标记 → 设标记 → 回补库存 + 用户计数
-- 消除 MQ 补偿与超时任务之间的双倍回补竞态窗口
--
-- KEYS[1] = flash:stock:{flashSaleId}
-- KEYS[2] = flash:user:{flashSaleId}
-- KEYS[3] = flash:compensated:{orderId}  （补偿标记，同时充当互斥锁）
-- ARGV[1] = userId
-- ARGV[2] = quantity（回补数量）
-- ARGV[3] = markerTtlSeconds（标记过期时间，秒）
-- 返回值:  1 = 补偿成功  /  0 = 已补偿过，跳过
-- ============================================================

-- 1. 检查是否已被补偿（MQ 补偿或超时任务可能先一步执行）
if redis.call('EXISTS', KEYS[3]) == 1 then
    return 0                                                    -- 已补偿，跳过
end

-- 2. 原子占位：先设标记再操作，后续调用者看到标记则跳过
redis.call('SET', KEYS[3], '1', 'EX', ARGV[3])                -- SET key 1 EX ttl

-- 3. 回补秒杀库存（INCRBY 是原子操作）
redis.call('INCRBY', KEYS[1], ARGV[2])                         -- 库存 +quantity

-- 4. 回退用户已购计数（HINCRBY 对不存在的 field 自动初始化为 0 再加）
redis.call('HINCRBY', KEYS[2], ARGV[1], -ARGV[2])              -- Hash field=userId 的值 -quantity

return 1                                                        -- 补偿成功
