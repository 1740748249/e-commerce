-- ============================================================
-- KEYS[1] = flash:stock:{flashSaleId}
-- KEYS[2] = flash:user:{flashSaleId}
-- ARGV[1] = userId
-- ARGV[2] = quantity（回补数量）
-- ============================================================

redis.call('INCRBY', KEYS[1], ARGV[2])
redis.call('HINCRBY', KEYS[2], ARGV[1], -ARGV[2])
return 1
