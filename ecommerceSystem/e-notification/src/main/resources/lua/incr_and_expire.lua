-- 原子执行 INCR + EXPIRE：每次 INCR 都刷新过期时间，确保活跃店铺的未读计数不会过期
local val = redis.call('INCR', KEYS[1])
redis.call('EXPIRE', KEYS[1], ARGV[1])
return val
