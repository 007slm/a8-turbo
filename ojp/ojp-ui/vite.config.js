import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    headers: {
      'X-Frame-Options': 'SAMEORIGIN'
    },
    proxy: {
      // 标准 HTTP API 直接访问 ojp-server
      '^/api/(?!grpc)': {
        target: 'http://localhost:8010',
        changeOrigin: true,
        secure: false,
      },
      '/actuator': {
        target: 'http://localhost:8010',
        changeOrigin: true,
        secure: false,
      },
      // gRPC 相关 API 走 Node.js 代理服务器
      '^/api/grpc': {
        target: 'http://localhost:50080',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path
      },
      // Grafana 监控面板代理
      '/grafana': {
        target: 'http://localhost:3000',
        changeOrigin: true,
        secure: false,
        configure: (proxy, options) => {
          proxy.on('proxyRes', (proxyRes, req, res) => {
            // 移除或修改 Grafana 的 X-Frame-Options 头部
            delete proxyRes.headers['x-frame-options'];
            // 设置允许同源嵌入
            proxyRes.headers['x-frame-options'] = 'SAMEORIGIN';
          });
        }
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})
