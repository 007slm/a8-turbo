import { Tooltip } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import React from 'react';

/**
 * 术语解释字典
 */
export const TERM_DEFINITIONS = {
    // 系统资源相关
    'cpu': 'CPU（中央处理器）使用率，表示系统处理任务的繁忙程度',
    'memory': '内存是计算机用于临时存储数据和程序的硬件设备',
    'heap': '堆内存是 Java 应用程序运行时存储对象的主要内存区域',
    'nonHeap': '非堆内存用于存储类定义、方法代码等元数据信息',
    'disk': '磁盘是计算机用于永久存储数据的设备',
    'uptime': '运行时长表示系统从启动到现在持续运行的时间',

    // 性能指标相关
    'qps': 'QPS（每秒查询数）表示系统每秒处理的请求数量，是衡量系统吞吐量的重要指标',
    'tps': 'TPS（每秒事务数）表示系统每秒完成的事务数量',
    'responseTime': '响应时间表示系统处理一个请求所需的时长，越低表示性能越好',
    'latency': '延迟是指从发送请求到收到响应的时间间隔',
    'throughput': '吞吐量表示系统在单位时间内处理的数据量或请求数',

    // Java/JVM 相关
    'jvm': 'JVM（Java 虚拟机）是运行 Java 应用程序的核心环境',
    'gc': 'GC（垃圾回收）是 JVM 自动清理不再使用的内存的过程',
    'thread': '线程是程序执行的最小单位，多线程可以提高程序并发处理能力',
    'youngGc': '新生代垃圾回收主要清理短期存活的对象',
    'fullGc': '完全垃圾回收会清理整个堆内存，通常耗时较长',

    // 数据库相关
    'connectionPool': '连接池是预先创建并维护的数据库连接集合，可以提高数据库访问效率',
    'activeConnections': '活跃连接数表示当前正在使用的数据库连接数量',
    'idleConnections': '空闲连接数表示连接池中可用但未使用的连接数量',

    // 缓存相关
    'cache': '缓存是将常用数据存储在快速访问的存储器中，以提高访问速度',
    'hitRate': '命中率表示从缓存中成功获取数据的请求占总请求的比例',
    'missRate': '未命中率表示需要从原始数据源获取数据的请求占总请求的比例',
    'eviction': '缓存淘汰是指当缓存满时，移除部分数据以腾出空间的过程',

    // 数据存储相关
    'dataWarehouse': '数据仓库是用于存储和分析大量历史数据的系统',
    'olap': 'OLAP（在线分析处理）用于复杂的数据分析和查询',
    'oltp': 'OLTP（在线事务处理）用于日常的事务性操作',

    // 监控相关
    'metrics': '指标是用于衡量系统性能和健康状况的数值',
    'timeSeries': '时间序列数据是按时间顺序记录的一系列数据点',
    'scrape': '采集是指定期从目标系统获取监控数据的过程',
    'target': '目标是被监控系统的端点或服务',

    // CDC 相关
    'cdc': 'CDC（变更数据捕获）是实时捕获数据库变更的技术',
    'stream': '数据流是连续传输的数据序列',
    'event': '事件是系统中发生的特定动作或状态变化',
};

/**
 * 带解释的术语组件
 * @param {string} term - 术语关键字
 * @param {string} children - 显示的文本
 * @param {boolean} showIcon - 是否显示图标
 */
export const TermWithTooltip = ({ term, children, showIcon = true }) => {
    const definition = TERM_DEFINITIONS[term];

    if (!definition) {
        return <span>{children}</span>;
    }

    return (
        <Tooltip title={definition}>
            <span style={{ cursor: 'help', borderBottom: '1px dashed #d9d9d9' }}>
                {children}
                {showIcon && (
                    <InfoCircleOutlined
                        style={{
                            marginLeft: 4,
                            fontSize: 12,
                            color: '#8c8c8c',
                            verticalAlign: 'middle'
                        }}
                    />
                )}
            </span>
        </Tooltip>
    );
};

/**
 * 服务名称映射（技术名称 -> 业务名称）
 */
export const SERVICE_NAME_MAP = {
    'prometheus': '监控服务',
    'redis': '数据同步服务',
    'starrocks': '数据仓库',
    'grafana': '可视化平台',
    'kafka': '消息队列',
    'mysql': '关系数据库',
    'ojp-server': 'OJP 服务端',
    'ojp-cache': '缓存服务',
};

/**
 * 获取业务友好的服务名称
 */
export const getBusinessName = (technicalName) => {
    return SERVICE_NAME_MAP[technicalName.toLowerCase()] || technicalName;
};

export default {
    TERM_DEFINITIONS,
    TermWithTooltip,
    SERVICE_NAME_MAP,
    getBusinessName,
};
