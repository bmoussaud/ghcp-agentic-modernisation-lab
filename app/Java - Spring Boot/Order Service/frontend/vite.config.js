import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The dev server proxies /api to the Spring Boot backend on :8080.
// This keeps the frontend identical before and after the backend modernization.
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    // Allow all hosts: needed when accessed through a forwarded/proxied URL
    // (e.g. devcontainer/Codespaces port forwarding), which rewrites the Host header.
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
