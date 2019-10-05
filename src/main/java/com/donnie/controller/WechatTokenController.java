package com.donnie.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.donnie.model.User;
import com.donnie.repository.mongo.MongoUserRepository;
import com.donnie.service.WechatTokenService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@RestController
public class WechatTokenController {
    @Resource
    private WechatTokenService tokenService;
    @Resource
    private MongoUserRepository mongoUserRepository;

    @RequestMapping("test")
    public JSONObject getToken(@RequestBody User user) {
        mongoUserRepository.save(user);
        User newUser = mongoUserRepository.findById(user.getId()).get();
        return (JSONObject) JSON.toJSON(newUser);
    }

    @RequestMapping("check")
    public JSONObject check(){
        return null;
    }
}
