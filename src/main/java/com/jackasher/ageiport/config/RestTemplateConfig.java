package com.jackasher.ageiport.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 * 为项目提供 HTTP 客户端支持
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建负载均衡的 RestTemplate
     * 支持服务发现和负载均衡
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 配置连接超时和读取超时
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 连接超时 5秒
        factory.setReadTimeout(10000);   // 读取超时 10秒
        
        restTemplate.setRequestFactory(factory);
        
        return restTemplate;
    }
}
