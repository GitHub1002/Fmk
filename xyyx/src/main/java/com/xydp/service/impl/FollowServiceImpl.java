package com.xydp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xydp.dto.Result;
import com.xydp.dto.UserDTO;
import com.xydp.entity.Follow;
import com.xydp.entity.User;
import com.xydp.mapper.FollowMapper;
import com.xydp.service.IFollowService;
import com.xydp.service.IUserService;
import com.xydp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 付淇
 * @version 1.0
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    /**
     * 关注业务逻辑
     *
     * @param followUserId
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        if (isFollow) {
            //关注,新增数据
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            boolean isSuccess = save(follow);
            if (isSuccess){
                String key = "follow:" + userId;
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }

        } else {
            //取消关注
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId).eq("follow_user_id", followUserId));
            if (isSuccess){
                String key = "follow:" + userId;
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询是否关注
     *
     * @param followUserId
     * @return
     */
    @Override
    public Result isFollow(Long followUserId) {
        // 查询是否关注
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        // 返回结果
        return Result.ok(count > 0);
    }

    /**
     * 查询关注的人
     *
     * @param id
     * @return
     */
    @Override
    public Result followCommons(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = "follow:" + userId;
        String key2 = "follow:" + id;
        // 2.求关注的人的交集
        Set<String> set = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (set.isEmpty() || set == null) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> userIds = set.stream().map(Long::valueOf).collect(Collectors.toList());
        // 3.查询用户信息
        List<UserDTO> userDTOList = userService.listByIds(userIds).stream()
                .map(e -> BeanUtil.copyProperties(e, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOList);
    }
}
