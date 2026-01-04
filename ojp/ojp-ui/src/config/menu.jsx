import React from 'react';
import {
    LayoutDashboard,
    Monitor,
    Database,
    Server,
    Activity,
    Zap,
    ShoppingBag,
    Users,
    Package,
    ShoppingCart,
    MessageSquare,
    FileCode,
    Grid
} from 'lucide-react';

export const menuItems = [
    {
        key: 'home',
        icon: <LayoutDashboard size={20} />,
        label: '监控总览',
        path: '/monitor'
    },
    {
        key: 'monitor-dashboard',
        icon: <Activity size={20} />,
        label: '服务监控',
        children: [
            {
                key: 'monitor-cache',
                icon: <Zap size={18} />,
                label: '缓存服务',
                path: '/monitor/cache'
            },
            {
                key: 'monitor-redis',
                icon: <Database size={18} />,
                label: '数据同步服务',
                path: '/monitor/redis'
            },
            {
                key: 'monitor-starrocks',
                icon: <Server size={18} />,
                label: '数据仓库',
                path: '/monitor/starrocks'
            },
            {
                key: 'monitor-prometheus',
                icon: <Monitor size={18} />,
                label: '监控服务',
                path: '/monitor/prometheus'
            },
        ]
    },
    {
        key: 'cache',
        icon: <Database size={20} />,
        label: '缓存管理',
        children: [
            {
                key: 'cache-recommendations',
                label: '智能推荐',
                icon: <Zap size={18} />,
                path: '/cache/recommendations'
            },
            {
                key: 'cache-rules',
                label: '缓存规则',
                path: '/cache/rules'
            },
            {
                key: 'cache-queries',
                label: '慢查询列表',
                path: '/cache/queries'
            }
        ]
    },
    {
        key: 'shopservice',
        icon: <ShoppingBag size={20} />,
        label: 'ShopService',
        children: [
            {
                key: 'shopservice-users',
                label: '用户管理',
                icon: <Users size={18} />,
                path: '/shopservice/users'
            },
            {
                key: 'shopservice-products',
                label: '商品管理',
                icon: <Package size={18} />,
                path: '/shopservice/products'
            },
            {
                key: 'shopservice-orders',
                label: '订单管理',
                icon: <ShoppingCart size={18} />,
                path: '/shopservice/orders'
            },
            {
                key: 'shopservice-reviews',
                label: '评价管理',
                icon: <MessageSquare size={18} />,
                path: '/shopservice/reviews'
            },
            {
                key: 'shopservice-chinook',
                label: 'Chinook SQL 实验台',
                icon: <FileCode size={18} />,
                path: '/shopservice/chinook'
            }
        ]
    },
    {
        key: 'service-portal',
        icon: <Grid size={20} />,
        label: '管理导航',
        path: '/service-portal'
    },
];
