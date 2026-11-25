<template>
  <view class="student-shell">
    <view class="safe-area" />
    <view class="topbar">
      <view>
        <text class="status">{{ schoolName || '高中生端' }} · 高三</text>
        <view class="brand"><text class="brand-mark">招</text><text>招生</text></view>
      </view>
    </view>

    <view class="hero">
      <text class="eyebrow">面向{{ schoolName || '你的学校' }}的专属咨询</text>
      <view class="hero-title-row"><text class="hero-title">这一志愿，先问问<br />真正走过的人。</text><view class="notice hero-notice" @tap="openNotifications" aria-label="消息通知"><view class="notice-bell" /><view v-if="unreadCount" class="notice-dot" /></view></view>
      <text class="hero-copy">把大学、专业和校园生活的问题，交给真实在读的学长学姐。</text>
      <view class="search-wrap"><text class="search-icon">⌕</text><input v-model="searchText" placeholder="搜索大学、专业或你关心的问题" /></view>
    </view>

    <view class="section recommendation-section">
      <view class="section-title-row"><text class="section-title">为你推荐的回答</text><text class="section-link" @tap="go('/pages/student/qa-forum')">查看全部 ›</text></view>
      <scroll-view scroll-x class="filter-scroll"><text v-for="item in answerFilters" :key="item" class="filter" :class="{ active: answerFilter === item }" @tap="answerFilter = item">{{ item }}</text></scroll-view>
      <view class="answer-list">
        <view v-for="answer in filteredAnswers" :key="answer.id" class="answer-row" @tap="openAnswer(answer)">
          <view class="avatar">{{ initials(answer) }}</view><view class="answer-main"><text class="answer-name">{{ answer.authorName || answer.userName || '学长学姐' }}</text><text class="answer-meta">{{ answer.universityName || '高校' }} · {{ answer.majorName || '专业经验' }}</text><text class="answer-topic">{{ answer.title || answer.questionTitle || '分享一段真实的大学体验' }}</text></view><text class="answer-arrow">›</text>
        </view>
        <view v-if="!filteredAnswers.length" class="empty">暂时没有匹配的回答，换个关键词试试。</view>
      </view>
    </view>

    <view class="section latest-section">
      <view class="section-title-row"><text class="section-title">大家最近都在问</text><text class="section-link" @tap="go('/pages/student/qa-forum')">进入问答 ›</text></view>
      <view v-for="item in latestQuestions" :key="item.id" class="question-row" @tap="openQuestion(item)"><text class="question-tag">{{ item.categoryName || '校园生活' }}</text><text class="question-title">{{ item.title || item.question }}</text><text class="question-meta">{{ item.answerCount || 0 }} 条讨论 · {{ item.authorType === 'volunteer' ? '大学生志愿者' : item.authorType === 'student' ? '高中生' : '' }}</text></view>
      <view v-if="!latestQuestions.length" class="empty">还没有公开讨论，去问答广场发起第一条帖子。</view>
    </view>

    <view class="quick-actions"><view @tap="go('/pages/student/university-list')"><text class="quick-icon">▦</text><text>找高校</text></view><view @tap="go('/pages/student/qa-forum')"><text class="quick-icon">＋</text><text>发起讨论</text></view><view @tap="go('/pages/student/my-questions')"><text class="quick-icon">◷</text><text>我的内容</text></view></view>

    <TabBar currentPath="/pages/student/home" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import * as studentApi from '@/api/student'
import * as qaApi from '@/api/qa'
import * as notificationApi from '@/api/notification'
import TabBar from '@/components/TabBar.vue'

const userStore = useUserStore()
const appStore = useAppStore()
const searchText = ref('')
const answerFilter = ref('推荐')
const answers = ref([])
const latestQuestions = ref([])
const unreadCount = ref(0)
const answerFilters = ['推荐', '计算机', '医学', '经管', '校园生活']
const schoolName = computed(() => appStore.selectedHighSchoolName || userStore.userInfo?.highSchoolName || '')
const filteredAnswers = computed(() => answers.value.filter(item => {
  const haystack = `${item.authorName || ''} ${item.universityName || ''} ${item.majorName || ''} ${item.title || ''} ${item.questionTitle || ''}`.toLowerCase()
  const queryMatch = !searchText.value.trim() || haystack.includes(searchText.value.trim().toLowerCase())
  const filterMatch = answerFilter.value === '推荐' || haystack.includes(answerFilter.value.toLowerCase()) || (answerFilter.value === '校园生活' && haystack.includes('校园'))
  return queryMatch && filterMatch
}))

onShow(loadData)
async function loadData() {
  try {
    const info = await studentApi.getMyInfo()
    if (info) userStore.updateUserInfo({ highSchoolId: info.highSchoolId, highSchoolName: info.highSchoolName, nickname: info.nickname || info.name, name: info.name, relatedId: info.id })
  } catch (error) { console.warn('学生信息暂时不可用', error) }
  try {
    const result = await qaApi.getFeaturedQA({ page: 1, size: 6 })
    answers.value = result?.records || result || []
  } catch (error) { answers.value = [] }
  try {
    const result = await qaApi.getForumPosts({ page: 1, size: 3 })
    latestQuestions.value = result?.records || result || []
  } catch (error) { latestQuestions.value = [] }
  try { unreadCount.value = await notificationApi.getUnreadCount() || 0 } catch (error) { unreadCount.value = 0 }
}
function initials(item) { return (item.authorName || item.userName || '学')[0] }
function go(url) { uni.redirectTo({ url }) }
function openNotifications() { uni.navigateTo({ url: '/pages/student/notifications' }) }
function openAnswer(item) { if (item.id) uni.navigateTo({ url: `/pages/student/forum-detail?id=${item.id}` }) }
function openQuestion(item) { if (item.id) uni.navigateTo({ url: `/pages/student/forum-detail?id=${item.id}` }) }
</script>

<style lang="scss" scoped>
.student-shell{min-height:100vh;padding-bottom:128rpx;background:#f3f7fb;color:#1f2937}.safe-area{height:env(safe-area-inset-top);background:#fff}.topbar{display:flex;justify-content:space-between;align-items:center;padding:16rpx 32rpx 20rpx;background:#fff}.status{display:block;color:#8a99a7;font-size:20rpx;margin-bottom:10rpx}.brand{display:flex;align-items:center;gap:12rpx;font-size:38rpx;font-weight:800}.brand-mark{display:grid;place-items:center;width:58rpx;height:58rpx;border-radius:14rpx;background:#5b9bd5;color:#fff}.notice{position:relative;display:grid;place-items:center;width:68rpx;height:68rpx;border-radius:50%;background:#eaf3fa;color:#397cba}.bell{font-size:40rpx;line-height:1;transform:rotate(-25deg)}.notice-dot{position:absolute;right:6rpx;top:5rpx;width:16rpx;height:16rpx;border-radius:50%;background:#ec6b67;border:3rpx solid #fff}.hero{padding:38rpx 32rpx 30rpx;background:#fff}.eyebrow{display:block;color:#397cba;font-size:22rpx;font-weight:700;margin-bottom:12rpx}.hero-title{display:block;font-size:48rpx;line-height:1.28;font-weight:800}.hero-copy{display:block;margin-top:14rpx;color:#728292;font-size:24rpx}.search-wrap{display:flex;align-items:center;gap:12rpx;margin-top:26rpx;padding:0 20rpx;height:86rpx;border-radius:22rpx;background:#edf5fb}.search-icon{font-size:34rpx;color:#397cba}.search-wrap input{flex:1;font-size:25rpx;color:#1f2937}.section{padding:34rpx 32rpx 0}.section-title-row{display:flex;justify-content:space-between;align-items:center;margin-bottom:18rpx}.section-title{font-size:32rpx;font-weight:800}.section-link{color:#397cba;font-size:23rpx}.filter-scroll{white-space:nowrap;margin:0 -32rpx;padding:0 32rpx 18rpx}.filter{display:inline-block;margin-right:12rpx;padding:13rpx 22rpx;border-radius:999rpx;background:#e8f2f9;color:#718394;font-size:23rpx}.filter.active{background:#5b9bd5;color:#fff;font-weight:700}.answer-list{background:#fff;border-radius:20rpx}.answer-row{display:flex;align-items:center;gap:18rpx;padding:24rpx;border-bottom:1rpx solid #edf2f6}.answer-row:last-child{border-bottom:0}.avatar{display:grid;place-items:center;flex:0 0 74rpx;height:74rpx;border-radius:50%;background:#dbeaf4;color:#397cba;font-size:28rpx;font-weight:800}.answer-main{flex:1;min-width:0}.answer-name,.answer-meta,.answer-topic{display:block}.answer-name{font-size:27rpx;font-weight:700}.answer-meta{margin-top:4rpx;color:#82909d;font-size:21rpx}.answer-topic{margin-top:9rpx;overflow:hidden;white-space:nowrap;text-overflow:ellipsis;color:#34495e;font-size:24rpx}.answer-arrow{color:#9aaab8;font-size:38rpx}.question-row{display:block;padding:22rpx 0;border-bottom:1rpx solid #dfe8ef}.question-tag{display:inline-block;color:#397cba;font-size:21rpx}.question-title{display:block;margin-top:8rpx;font-size:27rpx;font-weight:700}.question-meta{display:block;margin-top:8rpx;color:#8695a2;font-size:21rpx}.empty{padding:30rpx 18rpx;text-align:center;color:#8898a6;font-size:23rpx}.quick-actions{display:flex;justify-content:space-around;margin:34rpx 32rpx 0;padding:24rpx 10rpx;background:#fff;border-radius:20rpx}.quick-actions view{display:flex;flex-direction:column;align-items:center;gap:8rpx;color:#53697b;font-size:22rpx}.quick-icon{display:grid;place-items:center;width:64rpx;height:64rpx;border-radius:50%;background:#eaf3fa;color:#397cba;font-size:34rpx}
.safe-area{height:calc(env(safe-area-inset-top) + 16rpx);min-height:64rpx}.hero-title-row{display:flex;align-items:flex-start;justify-content:space-between;gap:24rpx}.hero-title{flex:1}.hero-notice{flex:none;transform:translateY(10rpx)}.notice-bell{position:relative;width:24rpx;height:27rpx;border:4rpx solid #397cba;border-bottom:0;border-radius:14rpx 14rpx 7rpx 7rpx}.notice-bell:before{position:absolute;right:-7rpx;bottom:-7rpx;left:-7rpx;height:4rpx;border-radius:4rpx;background:#397cba;content:''}.notice-bell:after{position:absolute;bottom:-12rpx;left:8rpx;width:8rpx;height:8rpx;border-radius:50%;background:#397cba;content:''}
</style>
