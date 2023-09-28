-- 1、参数列表
-- 1.1、优惠卷id
local voucherId = ARGV[1];
-- 1.2、用户id
local userId = ARGV[2];
-- 1.3、订单id
local orderId = ARGV[2];

-- 2. 库存key
local stockKey = "seckill:stock:" .. voucherId;
-- 2.1、优惠卷key
local voucherKey = "seckill:order:" .. voucherId;

-- 3.脚本业务
-- 3.1 判断是否有库存
if (tonumber(redis.call('get', stockKey))<= 0) then
    -- 3.1.1 没有库存
    return 1;
end

-- 3.2 判断是否已经抢购过
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 3.2.1 已经抢购过
    return 2;
end


-- 3.3 扣减库存
redis.call('incrby', voucherKey, -1);
-- 3.4 添加订单
redis.call('sadd', orderKey, userId);
-- 3.5 发送消息到消息队列
redis.call('xadd', 'seckill.order', '*', 'userId', userId, 'voucherId', voucherId, 'id',orderId);

return 0;
