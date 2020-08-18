package com.example.lock.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by zhangjiawen on 2019/8/13.
 */
@Component
public class LuaRedisSchedulePlus {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RedisTemplate redisTemplate;
    private static final String publicKey = "DEMO_COMMON_KEY";
    Boolean lock = false;
    private DefaultRedisScript<Boolean> lockScript;

    private Random random = new Random();

    @Scheduled(cron = "*/5 * * * * *")
    public void setNx() {

        String ipConfig = String.valueOf(random.nextInt(100));
        try {

            logger.info("ipConfig:" + ipConfig);

            lock = luaExpress(publicKey, "30", ipConfig);
            if (!lock) {
                String value = (String) redisTemplate.opsForValue().get(publicKey);
                logger.warn("key have exist belong to:" + value);
            } else {
                //获取锁成功
                logger.info("start lock lockNxExJob success");
                // Thread.sleep(5000);
            }
            Thread.sleep(6000);
        } catch (Exception e) {
            logger.error("setNx error!");
            e.printStackTrace();
        } finally {
            // redisTemplate.delete(publicKey);
            Boolean releaseResult = releaseLock(publicKey, ipConfig);
            if (!releaseResult) {
                logger.error("解锁失败");
            }
        }
    }

    @Scheduled(cron = "1/3 * * * * *")
    public void setNx2() {

        String ipConfig = String.valueOf(random.nextInt(100));
        try {

            logger.info("--ipConfig:" + ipConfig);

            lock = luaExpress(publicKey, "30", ipConfig);
            if (!lock) {
                String value = (String) redisTemplate.opsForValue().get(publicKey);
                logger.warn("--key have exist belong to:" + value);
            } else {
                //获取锁成功
                logger.info("--start lock lockNxExJob success");
                // Thread.sleep(5000);
            }
            Thread.sleep(6000);
        } catch (Exception e) {
            logger.error("--setNx error!");
            e.printStackTrace();
        } finally {
            // redisTemplate.delete(publicKey);
            Boolean releaseResult = releaseLock(publicKey, ipConfig);
            if (!releaseResult) {
                logger.error("--解锁失败");
            }
        }
    }

    /**
     * 获取lua结果
     *
     * @param key
     * @param value
     * @return
     */
    public Boolean luaExpress(String key, String time, String value) {
        lockScript = new DefaultRedisScript<Boolean>();
        lockScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("add.lua")));
        lockScript.setResultType(Boolean.class);
        // 封装参数
        List<Object> keyList = new ArrayList<Object>();
        keyList.add(key);
        keyList.add(time);
        keyList.add(value);
        Boolean result = (Boolean) redisTemplate.execute(lockScript, keyList);
        System.out.println(result);
        logger.info("redis set result：" + result);
        if (!result) {
            return false;
        }
        return true;
    }

    /**
     * 释放锁操作
     *
     * @param key
     * @param value
     * @return
     */
    private Boolean releaseLock(String key, String value) {
        lockScript = new DefaultRedisScript<Boolean>();
        lockScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("unlock.lua")));
        lockScript.setResultType(Boolean.class);
        // 封装参数
        List<Object> keyList = new ArrayList<Object>();
        keyList.add(key);
        keyList.add(value);
        Boolean result = (Boolean) redisTemplate.execute(lockScript, keyList);
        return result;
    }
}
