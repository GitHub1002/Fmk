# 校园点评
## 采用技术栈
Nginx+SpringBoot+Redis+MySql+Vue+Swagger+Lombok+MyBatis-Plus+Hutool
## 项目描述
项目主要为校园学生对周边店铺实现生活、娱乐、美食等多板块的信息分享类似大众点评，如附近的店铺，店铺优惠卷的抢购，用户能够购买平台店铺的优惠卷和商品，还可发表类似朋友圈的文章分享点赞，以及用户间的关注，心得文章信息流推送等。

## 项目技术特点
使用前后端分离技术，SpringBoot + Mybatis(Plus)快速构建项目实现统一化接口，整合Redis、Redisson等技术；  
使用Redis + ThreadLocal代替传统Session实现用户登录信息的共享和调用；  
采用 Redis 实现高频信息缓存，信息缓存采用主动更新策略与超时剔除策略相结合有效面对缓存穿透、缓存击穿和缓存雪崩等相关瓶颈问题，加快了请求响应速度，降低了 90%以上的数据库压力；  
使用Mysql乐观锁解决优惠卷秒杀超卖问题，对sql语句添加库存大于0条件对所有使用减少库存线程的操作加以限制；  
使用Redisson加锁实现一人一单操作，解决用户多次请求购买接口导致的一人多张重复优惠卷问题；  
使用Redis中的Stream消息队列 + Lua脚本实现对秒杀的异步优化，通过Lua脚本实现原子性判断当前用户是否符合购买资格，满足条件向MQ添加信息的操作；在项目启动时对当前业务开启一个线程任务循环监听MQ的中未被确定的生产者信息，并进行读取后下单；  
使用ZSET数据结构对文章的点赞信息进行统计排行，并限制单用户只能单次点赞同一篇文章；  
使用Feed流实现动态对关注的文章博主的最新文章进行滚动分页推送；  
