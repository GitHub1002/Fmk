package com.xydp;

import cn.hutool.log.Log;
import com.xydp.entity.Shop;
import com.xydp.entity.User;
import com.xydp.service.impl.ShopServiceImpl;
import com.xydp.utils.CacheClient;
import com.xydp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xydp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.xydp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    private CacheClient cacheClient;

    @Autowired
    private ShopServiceImpl service;

    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);

    @Test
    void testSave() {
        Shop byId = service.getById(1L);
        String key = CACHE_SHOP_KEY + 1L;
        cacheClient.setLogicalExpire(CACHE_SHOP_KEY + 1L, byId, 10L, TimeUnit.SECONDS);
    }

    @Test
    void testId() throws InterruptedException {
        CountDownLatch count = new CountDownLatch(300);
        Runnable tack = () -> {
            for (int i = 0; i < 100; i++) {
                long nextId = redisIdWorker.nextId("order");
                System.out.println("id:" + nextId);
            }
            count.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(tack);
        }
        count.await();
        long end = System.currentTimeMillis();
        System.out.println("总计耗时：" + (end - begin));

    }
    @Test
    void loadShopData() {
        // 1.查询店铺信息
        List<Shop> list = service.list();
        // 2.把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 3.分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 3.3.写入redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {
                // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }

}
