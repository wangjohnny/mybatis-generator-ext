package com.github.mybatis.generator.ext.demo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.mybatis.generator.ext.demo.mapper.UserMapper;
import com.github.mybatis.generator.ext.demo.model.User;
import com.github.mybatis.generator.ext.demo.model.UserExample;
import com.github.mybatis.generator.ext.demo.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<User> queryByExample(UserExample example) {
        return this.userMapper.selectByExample(null);
    }

    @Override
    public List<User> queryAll() {
        return this.userMapper.selectByExample(null);
    }

}
