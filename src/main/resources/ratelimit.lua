-- KEYS[1] will be the unique key for the endpoint, e.g., "ratelimit:getLimited"
-- ARGV[1] will be the refillRate
-- ARGV[2] will be the capacity
-- ARGV[3] will be the current timestamp from our Java app
-- ARGV[4] will be the number of tokens to consume (usually 1)

-- Step 1: Get all the arguments passed from the Java code
local key = KEYS[1]
local refill_rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- Step 2: Get the current state of the bucket from Redis
-- A Redis Hash is perfect for this. We'll store two fields: "tokens" and "ts" (timestamp)
local bucket = redis.call('HGETALL', key)
local last_tokens
local last_ts

-- Step 3: Check if the bucket even exists yet
if #bucket == 0 then
    -- It's the first request for this key. The bucket is full.
    last_tokens = capacity
    last_ts = now
else
    -- The bucket exists. We need to parse the values from the HGETALL result.
    -- HGETALL returns a list like {"tokens", "5", "ts", "167..."}, so we need to find the values.
    last_tokens = tonumber(bucket[2]) -- The value for "tokens"
    last_ts = tonumber(bucket[4])     -- The value for "ts" (timestamp)
end

-- Step 4: The refill logic. This is your Java math, now in Lua.
local time_passed = now - last_ts
local tokens_to_add = (time_passed * refill_rate) / 1000 -- Note: using integer math!

local current_tokens = math.min(capacity, last_tokens + tokens_to_add)

-- Step 5: The decision. Do we have enough tokens?
local new_tokens
if current_tokens >= requested then
    -- Yes, we have enough.
    new_tokens = current_tokens - requested

    -- Save the new state back to the Redis hash
    redis.call('HSET', key, 'tokens', new_tokens)
    redis.call('HSET', key, 'ts', now)

    -- Return 1 to indicate success
    return 1
else
    -- No, we don't have enough tokens.
    -- We don't change anything in Redis.
    -- Return 0 to indicate failure.
    return 0
end