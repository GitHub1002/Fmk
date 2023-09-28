package com.xydp.service;

import com.xydp.dto.Result;
import com.xydp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 付淇
 * @version 1.0
 */
public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryBlogById(Long id);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);

    Result queryBlogOfFollow(Long max, Integer offset);

    Result saveBlog(Blog blog);
}
