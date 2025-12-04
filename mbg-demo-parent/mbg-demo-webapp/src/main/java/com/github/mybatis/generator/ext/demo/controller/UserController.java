package com.github.mybatis.generator.ext.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.mybatis.generator.ext.demo.model.User;
import com.github.mybatis.generator.ext.demo.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/list", method = { RequestMethod.GET })
    @ResponseBody
    public List<User> list() {
        List<User> list = this.userService.queryAll();
        return list;
    }
}
