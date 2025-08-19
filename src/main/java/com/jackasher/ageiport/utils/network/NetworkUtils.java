package com.jackasher.ageiport.utils.network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网络工具类，用于获取正确的本地IP地址
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
public class NetworkUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);
    
    // 内网IP地址正则表达式
    private static final Pattern IP_PATTERN = Pattern.compile("^((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){3}(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])$");
    
    // 缓存获取到的IP地址，避免重复计算
    private static volatile String cachedLocalIP = null;
    
    /**
     * 获取本地真实的IP地址
     * 优先级：
     * 1. 内网IP（10.x.x.x, 172.16-31.x.x, 192.168.x.x）
     * 2. 公网IP
     * 3. 回环地址（作为最后的fallback）
     * 
     * @return 本地IP地址
     */
    public static String getLocalIP() {
        if (cachedLocalIP != null) {
            return cachedLocalIP;
        }
        
        synchronized (NetworkUtils.class) {
            if (cachedLocalIP != null) {
                return cachedLocalIP;
            }
            
            try {
                cachedLocalIP = doGetLocalIP();
                logger.info("获取到本地IP地址: {}", cachedLocalIP);
                return cachedLocalIP;
            } catch (Exception e) {
                logger.error("获取本地IP地址失败，使用回环地址作为备选", e);
                cachedLocalIP = "127.0.0.1";
                return cachedLocalIP;
            }
        }
    }
    
    /**
     * 强制刷新缓存的IP地址
     * 
     * @return 刷新后的IP地址
     */
    public static String refreshLocalIP() {
        synchronized (NetworkUtils.class) {
            cachedLocalIP = null;
            return getLocalIP();
        }
    }
    
    /**
     * 执行实际的IP获取逻辑
     */
    private static String doGetLocalIP() throws SocketException {
        String candidateIP = null;
        String intranetIP = null;
        String publicIP = null;
        
        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        
        while (netInterfaces.hasMoreElements()) {
            NetworkInterface ni = netInterfaces.nextElement();
            
            // 跳过回环接口、虚拟接口和无效接口
            if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                continue;
            }
            
            // 跳过Docker等虚拟网卡
            String name = ni.getName();
            if (name.startsWith("docker") || name.startsWith("br-") || 
                name.startsWith("veth") || name.startsWith("virbr")) {
                continue;
            }
            
            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                
                // 只处理IPv4地址
                if (!(address instanceof Inet4Address)) {
                    continue;
                }
                
                String ip = address.getHostAddress();
                
                // 跳过无效IP
                if (!isValidIP(ip)) {
                    continue;
                }
                
                // 跳过回环地址
                if (address.isLoopbackAddress()) {
                    continue;
                }
                
                logger.debug("发现网卡 {} 的IP地址: {}", name, ip);
                
                // 判断是否为内网IP
                if (isIntranetIP(ip)) {
                    intranetIP = ip;
                    logger.debug("发现内网IP: {}", ip);
                } else {
                    publicIP = ip;
                    logger.debug("发现公网IP: {}", ip);
                }
            }
        }
        
        // 优先返回内网IP，其次公网IP
        if (intranetIP != null) {
            return intranetIP;
        } else if (publicIP != null) {
            return publicIP;
        }
        
        // 如果都没有找到，使用连接外部地址的方式获取
        return getIPBySocket();
    }
    
    /**
     * 通过Socket连接的方式获取本地IP
     * 这种方式可以获取到真正用于对外通信的IP地址
     */
    private static String getIPBySocket() {
        try (Socket socket = new Socket()) {
            // 连接到一个外部地址（这里使用阿里云的DNS）
            socket.connect(new InetSocketAddress("223.5.5.5", 53), 3000);
            String ip = socket.getLocalAddress().getHostAddress();
            logger.debug("通过Socket连接获取到IP: {}", ip);
            return ip;
        } catch (Exception e) {
            logger.warn("通过Socket连接获取IP失败: {}", e.getMessage());
            return "127.0.0.1";
        }
    }
    
    /**
     * 验证IP地址格式是否有效
     */
    private static boolean isValidIP(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }
    
    /**
     * 判断是否为内网IP地址
     * 内网IP范围：
     * - 10.0.0.0 - 10.255.255.255
     * - 172.16.0.0 - 172.31.255.255  
     * - 192.168.0.0 - 192.168.255.255
     */
    private static boolean isIntranetIP(String ip) {
        if (!isValidIP(ip)) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        int a = Integer.parseInt(parts[0]);
        int b = Integer.parseInt(parts[1]);
        
        // 10.x.x.x
        if (a == 10) {
            return true;
        }
        
        // 172.16.x.x - 172.31.x.x
        if (a == 172 && b >= 16 && b <= 31) {
            return true;
        }
        
        // 192.168.x.x
        if (a == 192 && b == 168) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取本机的所有IP地址（用于调试）
     */
    public static void printAllNetworkInterfaces() {
        try {
            logger.info("=== 本机网络接口信息 ===");
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                logger.info("网卡名称: {} | 显示名称: {} | 是否启用: {} | 是否回环: {} | 是否虚拟: {}", 
                           ni.getName(), ni.getDisplayName(), ni.isUp(), ni.isLoopback(), ni.isVirtual());
                
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        String ip = address.getHostAddress();
                        logger.info("  -> IPv4地址: {} | 是否内网: {} | 是否回环: {}", 
                                   ip, isIntranetIP(ip), address.isLoopbackAddress());
                    }
                }
            }
            logger.info("=== 网络接口信息结束 ===");
        } catch (SocketException e) {
            logger.error("获取网络接口信息失败", e);
        }
    }
} 