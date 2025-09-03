import { defineConfig } from 'umi';

export default defineConfig({
  routes: [
    {
      path: '/',
      redirect: '/monitoring',
    },
    {
      name: '系统监控',
      path: '/monitoring',
      component: './Monitoring',
      icon: 'MonitorOutlined',
      routes: [
        {
          path: '/monitoring',
          redirect: '/monitoring/overview',
        },
        {
          name: '系统概览',
          path: '/monitoring/overview',
          component: './Monitoring',
        },
        {
          name: 'JVM信息',
          path: '/monitoring/jvm',
          component: './Monitoring',
        },
        {
          name: '内存使用',
          path: '/monitoring/memory',
          component: './Monitoring',
        },
        {
          name: '线程信息',
          path: '/monitoring/threads',
          component: './Monitoring',
        },
        {
          name: 'GC信息',
          path: '/monitoring/gc',
          component: './Monitoring',
        },
        {
          name: '数据库连接池',
          path: '/monitoring/dbpool',
          component: './Monitoring',
        },
        {
          name: 'HTTP指标',
          path: '/monitoring/http',
          component: './Monitoring',
        },
        {
          name: '业务指标',
          path: '/monitoring/business',
          component: './Monitoring',
        },
        {
          name: '自定义指标',
          path: '/monitoring/custom',
          component: './Monitoring',
        },
      ],
    },
    {
      name: '缓存管理',
      path: '/cache',
      component: './CacheManagement',
      icon: 'DatabaseOutlined',
    },
    {
      name: 'SQL统计',
      path: '/sql',
      component: './SqlStatistics',
      icon: 'BarChartOutlined',
    },
    {
      name: '系统测试',
      path: '/testing',
      component: './Testing',
      icon: 'BugOutlined',
    },
  ],
  npmClient: 'npm',
  mfsu: false,
});