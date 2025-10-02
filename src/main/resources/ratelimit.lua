-- KEYS[1] = The unique key for the rate limit (e.g., "ratelimit:my.endpoint")
-- ARGV[1] = The refill rate (tokens per second)
-- ARGV[2] = The capacity of the bucket
-- ARGV[3] = The current time in milliseconds from the client
-- ARGV[4] = The number of tokens to consume for this request (usually 1)

-- Convert ARGV values from strings to numbers for arithmetic
local rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- Get the current state of the bucket for the given key
-- HGETALL returns a list of key-value pairs, e.g., {"tokens", "5", "lastRefillTime", "167...etc"}
local bucket = redis.call('HGETALL', KEYS[1])

local last_tokens
local last_refill_time

-- If the bucket is empty, it's the first request for this key.
if #bucket == 0 then
  -- Initialize the bucket with full capacity and the current time.
  last_tokens = capacity
  last_refill_time = now
else
  -- The bucket exists, parse its values.
  -- HGETALL returns a flat list, so we iterate through it.
  for i = 1, #bucket, 2 do
    if bucket[i] == 'tokens' then
      last_tokens = tonumber(bucket[i+1])
    elseif bucket[i] == 'lastRefillTime' then
      last_refill_time = tonumber(bucket[i+1])
    end
  end
end

-- Calculate the time elapsed since the last refill
local elapsed = now - last_refill_time

if elapsed > 0 then
  -- Calculate how many new tokens should be added based on the elapsed time.
  -- We use pure integer math to avoid floating point issues.
  local tokens_to_add = math.floor((elapsed * rate) / 1000)

  -- Add the new tokens, but do not exceed the bucket's capacity.
  local current_tokens = math.min(last_tokens + tokens_to_add, capacity)

  -- Check if there are enough tokens to satisfy the request
  if current_tokens >= requested then
    -- Yes, there are enough tokens. Consume them.
    local new_tokens = current_tokens - requested

    -- Update the bucket in Redis with the new token count and the current time.
    -- Using HSET is better than HMSET which is now deprecated.
    redis.call('HSET', KEYS[1], 'tokens', new_tokens, 'lastRefillTime', now)

    -- Return 1 to signal success to the client.
    return 1
  else
    -- Not enough tokens. Do not update anything, just return failure.
    -- By not updating the lastRefillTime, we ensure the user gets their refill
    -- based on the last successful consumption or refill check.
    return 0
  end
else
  -- No time has passed (or clock went backwards), just check current tokens.
  if last_tokens >= requested then
    local new_tokens = last_tokens - requested
    redis.call('HSET', KEYS[1], 'tokens', new_tokens, 'lastRefillTime', now)
    return 1
  else
    return 0
  end
end
