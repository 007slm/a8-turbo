package org.openjdbcproxy.grpc.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DnsTest {
    public static void main(String[] args) {
        // 通过系统属性配置

        // 要解析的域名
        String domain = "mysql";

        try {
            // 使用系统默认 DNS 服务器解析域名
            InetAddress[] addresses = InetAddress.getAllByName(domain);

            System.out.println("解析域名: " + domain);
            System.out.println("找到 " + addresses.length + " 个 IP 地址:");

            for (InetAddress addr : addresses) {
                // 输出 IP 地址（不反向解析主机名）
                System.out.println("- " + addr.getHostAddress());
            }

        } catch (UnknownHostException e) {
            System.err.println("解析失败: 无法找到域名 '" + domain + "' 的 IP 地址");
            e.printStackTrace();
        }
    }
}
