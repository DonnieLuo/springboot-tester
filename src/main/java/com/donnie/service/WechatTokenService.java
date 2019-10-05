package com.donnie.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Service
public class WechatTokenService {
    private static final String WECHAT_ENDPOINT = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String GRANT_TYPE = "client_credential";
    private Logger logger = LoggerFactory.getLogger(WechatTokenService.class);
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RestTemplate restTemplate;


    public String getToken(String appId, String appSecret) {
        //cache
        String cacheKey = "wechattoken:" + appId;
        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return String.valueOf(cache);
        }
        //no cache
        else {
            String url = WECHAT_ENDPOINT + "?appid=" + appId + "&secret=" + appSecret + "&grant_type=" + GRANT_TYPE;
            WechatAccessToken wechatAccessToken = restTemplate.getForObject(url, WechatAccessToken.class);
            if (wechatAccessToken != null && !StringUtils.isEmpty(wechatAccessToken.getAccess_token())) {
                logger.info("refreshed wechat access token:{}", JSON.toJSONString(wechatAccessToken));
                cacheAccessToken(wechatAccessToken, cacheKey);
                return wechatAccessToken.getAccess_token();
            } else {
                logger.error("refresh wechat access token failed,{}", JSON.toJSONString(wechatAccessToken));
                return "";
            }
        }
    }

    /**
     * {
     * "errcode":0,
     * "errmsg":"ok",
     * "ticket":"bxLdikRXVbTPdHSM05e5u5sUoXNKd8-41ZO3MhKoyN5OfkWITDGgnr2fwJ0m9E8NYzWKVZvdVtaUgWvsdshFKA",
     * "expires_in":7200
     * }
     *
     * @param appId
     * @param appSecret
     * @return
     */
    public String getJsApiTicket(String appId, String appSecret) {
        String cacheK = "wxticket:" + appId;

        Object cache = redisTemplate.opsForValue().get(cacheK);
        if (cache != null) {
            return String.valueOf(cache);
        }


        Object token = getToken(appId, appSecret);
        String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=" + token + "&type=jsapi";

        JSONObject resp = restTemplate.getForObject(url, JSONObject.class);
        if (resp == null || resp.getIntValue("errcode") != 0) {
            logger.info("get js ticket failed.{}", JSON.toJSONString(resp));
            return "";
        }

        String ticket = resp.getString("ticket");
        redisTemplate.opsForValue().set(cacheK, ticket, 7000, TimeUnit.SECONDS);

        return ticket;
    }

    private void cacheAccessToken(WechatAccessToken wechatAccessToken, String cacheKey) {
        redisTemplate.opsForValue().set(cacheKey, wechatAccessToken.access_token, wechatAccessToken.getExpires_in(), TimeUnit.SECONDS);
    }

    private static class WechatAccessToken implements Serializable {
        private String access_token;
        private int expires_in;
        private int errcode;
        private String errmsg;

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }


        public int getErrcode() {
            return errcode;
        }

        public void setErrcode(int errcode) {
            this.errcode = errcode;
        }

        public String getErrmsg() {
            return errmsg;
        }

        public void setErrmsg(String errmsg) {
            this.errmsg = errmsg;
        }

        public int getExpires_in() {
            if (expires_in <= 0) {
                // default one hour
                expires_in = 60 * 60;
            } else {
                // else refresh expiry half an hour in advance.
                expires_in = expires_in - 1800;
            }

            return expires_in;
        }

        public void setExpires_in(int expires_in) {
            this.expires_in = expires_in;
        }
    }
}
