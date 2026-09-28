package com.docqa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 制度文档智能检索与问答系统 - 启动类
 * excludeName：排除运行环境注入的平台公共自动配置（miv-share-common），
 * 其 MybatisPlusInterceptor 等组件与本项目配置冲突。
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(excludeName = "cn.metaglobal.miv.share.common.MivShareCommonConfig")
@MapperScan("com.docqa.**.mapper")
public class DocQaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocQaApplication.class, args);
    }
}
