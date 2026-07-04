-- 原子初始化/覆盖 coupon:meta:{id} 的 stock 和 status
-- KEYS[1]: coupon:meta:{couponId}
-- ARGV[1]: stock
-- ARGV[2]: status
redis.call('HSET', KEYS[1], 'stock', ARGV[1], 'status', ARGV[2])
redis.call('EXPIRE', KEYS[1], 3600)
return 1
