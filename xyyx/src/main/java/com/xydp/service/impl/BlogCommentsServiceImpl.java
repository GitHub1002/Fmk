package com.xydp.service.impl;

import com.xydp.entity.BlogComments;
import com.xydp.mapper.BlogCommentsMapper;
import com.xydp.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 付淇
 * @version 1.0
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
