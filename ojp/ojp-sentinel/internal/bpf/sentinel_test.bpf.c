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
    
    // 简化测试：丢弃所有 TCP 包
    bpf_printk("TEST: Dropping ALL TCP packets!");
    return TC_ACT_SHOT;
}

char _license[] SEC("license") = "GPL";
