import { ref } from 'vue'

/**
 * 设备检测 — 响应式判断移动端
 * 模块级立即检测（vue setup 阶段即可用）
 */
const isMobile = ref(false)

// 浏览器环境立即检测
if (typeof window !== 'undefined') {
  const check = () => {
    isMobile.value = window.innerWidth < 768
  }
  check()
  window.addEventListener('resize', check)
}

export function useDevice() {
  return { isMobile }
}

export { isMobile }
