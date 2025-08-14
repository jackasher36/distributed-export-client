package com.jackasher.ageiport.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jackasher
 * @version 1.0
 * @className AgeiPortProperties
 * @since 1.0
 **/
@Configuration
@ConfigurationProperties(prefix = "ageiport")
@Data
public class AgeiPortProperties {

    @Value("${ageiport.taskServerClientOptions.port}")
    private Integer port;

    @Value("${ageiport.taskServerClientOptions.endpoint}")
    private String endpoint;


}
