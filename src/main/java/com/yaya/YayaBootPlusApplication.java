package com.yaya;

import com.github.jeffreyning.mybatisplus.conf.EnableMPP;
import com.jthinking.common.util.ip.IPInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@SpringBootApplication
@EnableMPP //开启mybatisplus-plus依赖功能,核心是补全mybatis-plus不支持联合主键的问题
@MapperScan("com.yaya.**.mapper")
public class YayaBootPlusApplication {

    static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(YayaBootPlusApplication.class, args);
        //初始化ip地理信息库
        log.info("IP地理信息数据库组件启动中...");
        IPInfoUtils.init();
        log.info("IP地理信息数据库组件启动完成...");
        log.info("服务器启动成功... 启动截止时间:{}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        //获取环境配置信息
        ConfigurableEnvironment environment = context.getEnvironment();
        //日志保存地址
        String logPath = environment.getProperty("yaya.log-path");
        //文件上传地址
        String filePath = environment.getProperty("yaya.file-path");
        //环境
        String active = environment.getProperty("spring.profiles.active");
        log.info("当前active:{},日志保存地址:{},文件上传地址:{}",active,logPath,filePath);
    }

}
