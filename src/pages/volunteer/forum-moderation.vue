<template>
  <view class="moderation">
    <NavBar title="本校论坛审核" />
    <view class="moderation__tabs">
      <view v-for="tab in tabs" :key="tab.value" :class="['moderation__tab', active === tab.value && 'moderation__tab--active']" @tap="switchTab(tab.value)">{{ tab.label }}</view>
    </view>

    <view class="moderation__item" v-for="item in list" :key="item.id">
      <template v-if="active === 'words'">
        <text class="moderation__title">{{ item.word }}</text>
        <view class="moderation__actions"><button class="danger" @tap="deleteWord(item)">删除</button></view>
      </template>
      <template v-else-if="active === 'posts'">
        <text class="moderation__title">{{ item.title || '未命名帖子' }}</text>
        <text class="moderation__content">{{ item.questionContent }}</text>
        <view class="moderation__actions">
          <button @tap="reviewPost(item, 'REJECTED')">拒绝</button>
          <button class="primary" @tap="reviewPost(item, 'APPROVED')">通过</button>
        </view>
      </template>
      <template v-else-if="active === 'featured'">
        <text class="moderation__title">{{ item.title || '未命名帖子' }}</text>
        <text class="moderation__content">热度 {{ item.heatScore || 0 }} · {{ item.questionContent }}</text>
        <view class="moderation__actions">
          <text v-if="item.authorType === 'student'" class="moderation__featured-forbidden">高中生内容不可设精华</text>
          <button v-else class="primary" @tap="feature(item)">{{ item.isFeatured ? '取消精华' : '设为精华' }}</button>
        </view>
      </template>
      <template v-else-if="active === 'reports'">
        <view class="moderation__report-post">
          <view class="moderation__report-header">
            <text class="moderation__report-label">{{ item.targetType === 'comment' ? '被举报评论' : '原帖内容' }}</text>
            <text class="moderation__report-id">{{ item.targetType === 'comment' ? '评论 #' + item.commentId : '帖子 #' + item.postId }}</text>
          </view>
          <template v-if="item.targetType === 'comment'">
            <text class="moderation__report-title">{{ item.commentAuthorName ? '评论人：' + item.commentAuthorName : '评论人：未知用户' }}</text>
            <text class="moderation__report-body">{{ item.commentContent || '评论内容不可用' }}</text>
            <text class="moderation__report-context">所在帖子：{{ item.postTitle || '无标题帖子' }}</text>
          </template>
          <template v-else>
            <text class="moderation__report-title">{{ item.postTitle || '无标题帖子' }}</text>
            <text class="moderation__report-body">{{ item.postContent || '原帖内容不可用' }}</text>
          </template>
        </view>
        <view class="moderation__report-reason">
          <text class="moderation__report-label">举报理由</text>
          <text class="moderation__content">{{ item.reason }}{{ item.detail ? '：' + item.detail : '' }}</text>
        </view>
        <view class="moderation__actions">
          <button @tap="reviewReport(item, false)">不成立</button>
          <button class="danger" @tap="reviewReport(item, true)">成立并禁言</button>
        </view>
      </template>
      <template v-else>
        <text class="moderation__title">处罚申诉 #{{ item.penaltyId }}</text>
        <text class="moderation__content">{{ item.reason }}</text>
        <view class="moderation__actions">
          <button @tap="reviewAppeal(item, false)">驳回</button>
          <button class="primary" @tap="reviewAppeal(item, true)">通过</button>
        </view>
      </template>
    </view>
    <view class="moderation__word-add" v-if="active === 'words'">
      <input v-model="newWord" maxlength="100" placeholder="新增屏蔽词" />
      <button @tap="addWord">添加</button>
    </view>
    <view class="moderation__empty" v-if="!list.length">暂无待处理内容</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import * as forumApi from '@/api/forum'
import * as adminApi from '@/api/admin'
import { useUserStore } from '@/store/modules/user'
const userStore = useUserStore()
const tabs = [{ label: '帖子', value: 'posts' }, { label: '精华', value: 'featured' }, { label: '举报', value: 'reports' }, { label: '申诉', value: 'appeals' }]
if (userStore.isSuperAdmin) tabs.push({ label: '屏蔽词', value: 'words' })
const active = ref('posts')
const list = ref([])
const newWord = ref('')
// 分页与竞态控制：触底加载下一页；requestSeq 保证快速切换 tab/操作时旧响应不覆盖新结果
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
let requestSeq = 0
const PAGE_SIZE = 20

onShow(() => {
  if (userStore.isTeacher && !userStore.hasPermission('teacherForumModeration')) {
    uni.showToast({ title: '论坛治理权限已被撤销', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 600)
    return
  }
  load(true)
})
async function load(reset = false) {
  if (loading.value || (!hasMore.value && !reset)) return
  loading.value = true
  const seq = ++requestSeq
  const targetPage = reset ? 1 : page.value
  try {
    if (active.value === 'words') {
      if (seq !== requestSeq) return
      list.value = (await adminApi.getSensitiveWords()) || []
      return
    }
    const api = userStore.isSuperAdmin
      ? (active.value === 'posts' ? adminApi.getForumPendingPosts : active.value === 'featured' ? adminApi.getForumFeaturedCandidates : active.value === 'reports' ? adminApi.getForumReports : adminApi.getForumAppeals)
      : (active.value === 'posts' ? forumApi.getPendingPosts : active.value === 'featured' ? forumApi.getFeaturedCandidates : active.value === 'reports' ? forumApi.getPendingReports : forumApi.getPendingAppeals)
    const data = await api({ page: targetPage, size: PAGE_SIZE })
    if (seq !== requestSeq) return
    const records = data?.records || []
    list.value = reset ? records : [...list.value, ...records]
    hasMore.value = records.length >= PAGE_SIZE
    page.value = targetPage + 1
    if (!reset && records.length === 0) hasMore.value = false
  } catch (e) {
    if (seq === requestSeq) uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
function onReachBottom() {
  load(false)
}
function switchTab(tab) { active.value = tab; load(true) }
async function addWord() { if (!newWord.value.trim()) return; try { await adminApi.addSensitiveWord(newWord.value.trim()); newWord.value = ''; load(true) } catch (e) { uni.showToast({ title: e.message || '添加失败', icon: 'none' }) } }
async function deleteWord(item) { try { await adminApi.deleteSensitiveWord(item.id); load(true) } catch (e) { uni.showToast({ title: e.message || '删除失败', icon: 'none' }) } }
async function reviewPost(item, action) { try { await (userStore.isSuperAdmin ? adminApi.reviewForumPost : forumApi.reviewPost)(item.id, action); uni.showToast({ title: '审核完成', icon: 'success' }); load(true) } catch (e) { uni.showToast({ title: e.message || '操作失败', icon: 'none' }) } }
async function feature(item) { try { await (userStore.isSuperAdmin ? adminApi.setForumFeatured : forumApi.setFeatured)(item.id, !item.isFeatured); uni.showToast({ title: '精华状态已更新', icon: 'success' }); load(true) } catch (e) { uni.showToast({ title: e.message || '操作失败', icon: 'none' }) } }
async function reviewReport(item, founded) { try { await (userStore.isSuperAdmin ? adminApi.reviewForumReport : forumApi.reviewReport)(item.id, { founded }); uni.showToast({ title: '处理完成', icon: 'success' }); load(true) } catch (e) { uni.showToast({ title: e.message || '操作失败', icon: 'none' }) } }
function reviewAppeal(item, approved) {
  const complete = async (restorePost) => { try { await (userStore.isSuperAdmin ? adminApi.reviewForumAppeal : forumApi.reviewAppeal)(item.id, { approved, restorePost }); uni.showToast({ title: '复核完成', icon: 'success' }); load(true) } catch (e) { uni.showToast({ title: e.message || '操作失败', icon: 'none' }) } }
  if (!approved) return complete(false)
  const isComment = item.targetType === 'comment'
  uni.showModal({ title: '通过申诉', content: isComment ? '是否同时恢复该评论显示？' : '是否同时恢复原帖子公开状态？', confirmText: isComment ? '恢复评论' : '恢复帖子', cancelText: '仅撤处罚', success: res => complete(res.confirm) })
}
</script>

<style lang="scss" scoped>
.moderation { min-height: 100vh; background: #f3f6f9; }
.moderation__tabs { display: flex; height: 82rpx; background: #fff; border-bottom: 1rpx solid #e2e8f0; }
.moderation__tab { flex: 1; text-align: center; line-height: 82rpx; color: #64748b; font-size: 26rpx; }.moderation__tab--active { color: #1d4ed8; border-bottom: 4rpx solid #1d4ed8; }
.moderation__item { margin: 18rpx 24rpx; padding: 24rpx; background: #fff; border-radius: 8rpx; }
.moderation__title { display: block; font-size: 28rpx; font-weight: 600; color: #172033; }.moderation__content { display: block; margin: 14rpx 0 20rpx; font-size: 25rpx; line-height: 1.55; color: #536174; }
.moderation__report-post { padding: 18rpx; background: #f8fafc; border-left: 6rpx solid #2563eb; }
.moderation__report-header { display: flex; justify-content: space-between; align-items: center; }
.moderation__report-label { font-size: 24rpx; font-weight: 600; color: #334155; }
.moderation__report-id { font-size: 22rpx; color: #94a3b8; }
.moderation__report-title { display: block; margin-top: 14rpx; font-size: 28rpx; font-weight: 600; color: #172033; }
.moderation__report-body { display: block; margin-top: 12rpx; font-size: 25rpx; line-height: 1.6; color: #475569; white-space: pre-wrap; }
.moderation__report-context { display: block; margin-top: 10rpx; font-size: 22rpx; color: #94a3b8; }
.moderation__report-reason { margin-top: 20rpx; }
.moderation__actions { display: flex; justify-content: flex-end; gap: 12rpx; }.moderation__actions button { margin: 0; padding: 0 26rpx; height: 64rpx; line-height: 64rpx; font-size: 24rpx; border-radius: 6rpx; }.moderation__actions .primary { color: #fff; background: #2563eb; }.moderation__actions .danger { color: #fff; background: #b91c1c; }
.moderation__featured-forbidden { align-self: center; font-size: 24rpx; color: #94a3b8; }
.moderation__empty { padding: 120rpx 0; text-align: center; color: #94a3b8; }
.moderation__word-add { position: sticky; bottom: 0; display: flex; gap: 12rpx; padding: 18rpx 24rpx; background: #fff; border-top: 1rpx solid #e2e8f0; }.moderation__word-add input { flex: 1; height: 68rpx; padding: 0 18rpx; background: #f1f5f9; font-size: 24rpx; }.moderation__word-add button { margin: 0; width: 120rpx; height: 68rpx; line-height: 68rpx; border-radius: 6rpx; font-size: 24rpx; color: #fff; background: #2563eb; }
</style>
