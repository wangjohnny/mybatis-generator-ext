package com.github.mybatis.generator.ext.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.github.mybatis.generator.ext.demo.model.User;
import com.github.mybatis.generator.ext.demo.model.UserExample;

@Service
public interface UserService {

    List<User> queryAll();

    List<User> queryByExample(UserExample example);

}
