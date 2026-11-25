<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import { initGlobalErrorReport } from '@/utils/report'

onLaunch(() => {
  console.log('App Launch')

  // 初始化全局错误监听（上报到 Sentry）
  initGlobalErrorReport()

  // 初始化系统信息（NavBar 需要状态栏高度）
  const appStore = useAppStore()
  appStore.initSystemInfo()

  // 恢复登录态
  const userStore = useUserStore()
  userStore.initFromStorage()
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style lang="scss">
page {
  background-color: #F2F6FB;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

view, text, image {
  box-sizing: border-box;
}

.container {
  padding: 24rpx;
}
</style>
