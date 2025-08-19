// src/main/java/com/jackasher/ageiport/config/AgeiPortConfig.java
package com.jackasher.ageiport.config;

import com.alibaba.ageiport.ext.cluster.SpringCloudClusterOptions;
import com.alibaba.ageiport.ext.cluster.SpringCloudNode;
import com.alibaba.ageiport.processor.core.AgeiPort;
import com.alibaba.ageiport.processor.core.AgeiPortOptions;
import com.alibaba.ageiport.processor.core.client.http.HttpTaskServerClientOptions;
import com.jackasher.ageiport.config.filestore.FileStoreOptionsFactory;
import com.jackasher.ageiport.constant.MainTaskCallbackConstant;
import com.jackasher.ageiport.utils.network.NetworkUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.UUID;

@Configuration
public class AgeiPortConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Resource
    private AgeiPortProperties ageiPortProperties;

    @Resource
    private ConfigurableApplicationContext applicationContext;

    @Resource
    private DiscoveryClient discoveryClient;

    @Resource
    private FileStoreOptionsFactory fileStoreOptionsFactory;

    /**
     * 步骤1：创建一个 AgeiPortOptions 的 Bean。
     * 这个方法只负责组装配置，不触发 AGEIPort 核心的初始化，因此是安全的。
     * 它不依赖任何其他复杂的 Bean，所以会优先被 Spring 创建。
     */
    @Bean
    public AgeiPortOptions ageiPortOptions() {
        AgeiPortOptions options = new AgeiPortOptions();
        // 设置命名空间
        options.setNamespace("jackasher");
        options.setAccessKeyId("your_access_key");
        options.setAccessKeySecret("your_access_key_secret");

        //文件存储配置
        options.setFileStoreOptions(fileStoreOptionsFactory.createFileStoreOptions());

        //集群配置
        SpringCloudNode localNode = new SpringCloudNode();
        localNode.setGroup(applicationName);
        localNode.setHost(NetworkUtils.getLocalIP());
        localNode.setId(UUID.randomUUID().toString());
        localNode.setApp(applicationName);
        localNode.setLabels(new HashMap<>());

        SpringCloudClusterOptions clusterOptions = new SpringCloudClusterOptions();
        clusterOptions.setDiscoveryClient(discoveryClient);
        clusterOptions.setApplicationContext(applicationContext);
        clusterOptions.setLocalNode(localNode);

        options.setClusterOptions(clusterOptions);
        options.setApp(applicationName);

        // 创建taskserver客户端配置
        HttpTaskServerClientOptions taskServerClientOptions = new HttpTaskServerClientOptions();
        taskServerClientOptions.setPort(ageiPortProperties.getPort());
        taskServerClientOptions.setEndpoint(ageiPortProperties.getEndpoint());
        options.setTaskServerClientOptions(taskServerClientOptions);

        // 回调配置
        options.setMainTaskCallback(MainTaskCallbackConstant.MAIN_TASK_CALLBACK_BEAN_NAME);

        return options;
    }

    /**
     * 步骤2：创建 AgeiPort Bean，并注入上面创建好的 AgeiPortOptions Bean。
     * 当 Spring 执行这个方法时，所有其他的 Bean（包括 mainTaskCallback）都已经被创建好了。
     * 所以当 AgeiPort.ageiPort(options) 执行初始化时，SpringExtensionFactory 能够成功找到 mainTaskCallback Bean。
     *
     * @param options 由 ageiPortOptions() 方法创建并注入的配置对象
     * @return 完全初始化的 AgeiPort 实例
     */
    @Bean
    public AgeiPort ageiPort(AgeiPortOptions options) {
        return AgeiPort.ageiPort(options);
    }
}
