-- 批量 INCR + EXPIRE：对所有 key 原子递增并刷新过期时间
for i = 1, #KEYS do
    redis.call('INCR', KEYS[i])
    redis.call('EXPIRE', KEYS[i], ARGV[1])
end
return #KEYS
