import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
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
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})
