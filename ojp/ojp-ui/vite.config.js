import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    host: '0.0.0.0',
    strictPort: true,
    headers: {
      'X-Frame-Options': 'SAMEORIGIN'
    },
    proxy: {
      // 标准 HTTP API 直接访问 ojp-server
      '/api': {
        target: 'http://localhost:8010',
        changeOrigin: true,
        secure: false,
      },
      // Grafana 代理
      '/grafana': {
        target: 'http://localhost:3000',
        changeOrigin: true,
        secure: false,
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            proxyReq.setHeader('X-Forwarded-For', req.connection.remoteAddress)
          })
        }
      },
      // Prometheus Proxy (via Kong)
      '/prometheus': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        secure: false,
      },
      '/shopservice/': {
        target: 'http://localhost:8180',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/shopservice/, '/')
      },
      // Loki Proxy
      '/loki': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/loki/, '/loki')
      },
      // Tempo Proxy
      '/tempo': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/tempo/, '/tempo')
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})