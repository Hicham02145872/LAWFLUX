import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'LAWFLUX_')
  const proxy = {
    '/api': { target: env.LAWFLUX_BFF_URL || 'http://127.0.0.1:8080' },
  }

  return {
    plugins: [react()],
    server: { host: '127.0.0.1', proxy },
    preview: { host: '127.0.0.1', proxy },
  }
})
