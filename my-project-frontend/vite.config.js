import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { VantResolver } from '@vant/auto-import-resolver'
// import { VitePWA } from 'vite-plugin-pwa'  // PWA 需要 Node 20+

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    host: '127.0.0.1',
    port: 5273,
  },
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver(), VantResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver(), VantResolver()],
    }),
    // PWA 支持（需 Node 20+，当前环境暂禁用）
    // VitePWA({
    //   registerType: 'autoUpdate',
    //   manifest: {
    //     name: '北梨论坛',
    //     short_name: '北梨论坛',
    //     description: '校园论坛社区',
    //     theme_color: '#1989fa',
    //     display: 'standalone',
    //     background_color: '#ffffff',
    //     icons: [{ src: '/src/assets/logo.svg', sizes: '192x192', type: 'image/svg+xml' }]
    //   }
    // }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
