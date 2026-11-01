import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  envPrefix: 'VITE_',
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
  },
})
