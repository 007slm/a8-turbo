#include <linux/bpf.h>
#include <linux/pkt_cls.h>
#include <linux/if_ether.h>
#include <linux/ip.h>
#include <linux/in.h>
#include <linux/tcp.h>
#include <linux/types.h>
#include <bpf/bpf_helpers.h>
#include <bpf/bpf_endian.h>

/* 定义授权状态结构 */
struct license_config {
    __u32 is_valid;    // 1: 有效, 0: 无效
    __u32 reserved;    // 保留字段
};

/* 共享内存 Map: 由 Go 用户态写入，C 内核态读取 */
struct {
    __uint(type, BPF_MAP_TYPE_HASH);
    __uint(max_entries, 1);
    __type(key, __u32);
    __type(value, struct license_config);
} license_map SEC(".maps");

/* 检查是否是 HTTP 请求 */
static __always_inline int is_http_request(void *data, void *data_end, 
                                           struct tcphdr *tcp) {
    // 计算 TCP payload 起始位置
    __u32 tcp_hdr_len = tcp->doff * 4;
    void *payload = (void *)tcp + tcp_hdr_len;
    
    // 边界检查：至少需要 4 字节来检查 HTTP 方法
    if (payload + 4 > data_end)
        return 0;
    
    char *p = (char *)payload;
    
    // 检查常见的 HTTP 方法
    // GET
    if (p[0] == 'G' && p[1] == 'E' && p[2] == 'T' && p[3] == ' ')
        return 1;
    // POST
    if (p[0] == 'P' && p[1] == 'O' && p[2] == 'S' && p[3] == 'T')
        return 1;
    // PUT
    if (p[0] == 'P' && p[1] == 'U' && p[2] == 'T' && p[3] == ' ')
        return 1;
    // DELETE (需要检查更多字节)
    if (payload + 6 <= data_end) {
        if (p[0] == 'D' && p[1] == 'E' && p[2] == 'L' && 
            p[3] == 'E' && p[4] == 'T' && p[5] == 'E')
            return 1;
    }
    // PATCH
    if (payload + 5 <= data_end) {
        if (p[0] == 'P' && p[1] == 'A' && p[2] == 'T' && 
            p[3] == 'C' && p[4] == 'H')
            return 1;
    }
    // OPTIONS
    if (payload + 7 <= data_end) {
        if (p[0] == 'O' && p[1] == 'P' && p[2] == 'T' && 
            p[3] == 'I' && p[4] == 'O' && p[5] == 'N' && p[6] == 'S')
            return 1;
    }
    // HEAD
    if (p[0] == 'H' && p[1] == 'E' && p[2] == 'A' && p[3] == 'D')
        return 1;
    
    return 0;
}

SEC("classifier")
int sentinel_ingress(struct __sk_buff *skb) {
    void *data_end = (void *)(long)skb->data_end;
    void *data = (void *)(long)skb->data;
    
    // 1. 解析以太网头
    struct ethhdr *eth = data;
    if ((void *)(eth + 1) > data_end)
        return TC_ACT_OK;
    
    // 只处理 IPv4 包
    if (eth->h_proto != bpf_htons(ETH_P_IP))
        return TC_ACT_OK;
    
    // 2. 解析 IP 头
    struct iphdr *ip = (void *)(eth + 1);
    if ((void *)(ip + 1) > data_end)
        return TC_ACT_OK;
    
    // 只处理 TCP 包
    if (ip->protocol != IPPROTO_TCP)
        return TC_ACT_OK;
    
    // 3. 解析 TCP 头
    __u32 ip_hdr_len = ip->ihl * 4;
    struct tcphdr *tcp = (void *)ip + ip_hdr_len;
    if ((void *)(tcp + 1) > data_end)
        return TC_ACT_OK;
    
    // 记录：收到 TCP 包
    bpf_printk("OJP-Sentinel: TCP packet detected, src_port=%d, dst_port=%d", 
               bpf_ntohs(tcp->source), bpf_ntohs(tcp->dest));
    
    // 4. 查询授权状态
    __u32 key = 0;
    struct license_config *config = bpf_map_lookup_elem(&license_map, &key);
    
    // 如果没有配置或授权有效，放行所有流量
    if (!config || config->is_valid == 1) {
        bpf_printk("OJP-Sentinel: License VALID, allowing traffic");
        return TC_ACT_OK;
    }
    
    /* 
     * 授权无效的情况：
     * 每分钟的最后 30 秒（30-59秒）丢弃所有 TCP 流量
     * 其他时间正常通过 (0-29秒)
     * 这样系统有 50% 的时间可用
     */
    
    // 获取当前时间（纳秒）
    __u64 now_ns = bpf_ktime_get_ns();
    // 转换为秒
    __u64 now_sec = now_ns / 1000000000;
    // 获取当前秒数在一分钟内的位置 (0-59)
    __u32 sec_in_minute = now_sec % 60;
    
    // 如果在最后 30 秒（30-59秒），丢弃流量
    if (sec_in_minute >= 30) {
        bpf_printk("OJP-Time: sec=%d >= 30, DROPPING", sec_in_minute);
        return TC_ACT_SHOT;
    }
    
    // 其他时间放行
    bpf_printk("OJP-Time: sec=%d < 30, allowing", sec_in_minute);
    return TC_ACT_OK;
}

char _license[] SEC("license") = "GPL";
