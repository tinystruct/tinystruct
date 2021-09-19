local key = KEYS[1]
local content = KEYS[2]
local ttl = ARGV[1]
local lock_set = redis.call('setnx', key, content)
if lock_set == 1 then
    redis.call('pexpire', key, ttl)
else
    local value = redis.call('get', key)
    if(value == content) then
        lock_set = 1; redis.call('pexpire', key, ttl)
    end
end
return lock_set;