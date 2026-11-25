<template>
  <view class="detail">
    <!-- 导航栏 -->
    <view class="detail__navbar" :style="{ paddingRight: (menuButtonWidth + 24) + 'px' }">
      <view class="detail__back" @tap="goBack">
        <text class="detail__back-icon">‹</text>
      </view>
      <text class="detail__navbar-title">帖子详情</text>
      <view class="detail__navbar-right" @tap="reportPost"><text>举报</text></view>
    </view>

    <!-- 帖子内容 -->
    <view class="detail__post" v-if="post">
      <!-- 作者信息 -->
      <view class="detail__author">
        <view :class="['detail__avatar', post.authorRole === 'volunteer' && 'detail__avatar--volunteer']">
          <text class="detail__avatar-text">{{ post.authorName?.charAt(0) || '匿' }}</text>
        </view>
        <view class="detail__author-info">
              <text class="detail__author-name">{{ formatIdentityName(post.authorName, post.authorRole) }}</text>
          <view class="detail__author-tag" v-if="post.authorRole === 'volunteer'">
            <text class="detail__author-tag-text">{{ post.authorUniversity || '大学生' }}</text>
          </view>
          <view class="detail__author-tag detail__author-tag--student" v-else>
            <text class="detail__author-tag-text">{{ post.authorSchool || '高中生' }}</text>
          </view>
        </view>
        <text class="detail__post-time">{{ formatTime(post.createdAt) }}</text>
      </view>

      <!-- 帖子标题和内容 -->
      <view class="detail__title-row">
        <text class="detail__title">{{ post.title }}</text>
        <text class="detail__featured" v-if="post.isFeatured">精华</text>
      </view>
      <text class="detail__content">{{ post.content }}</text>

      <!-- 帖子标签 -->
      <view class="detail__tags" v-if="post.tags && post.tags.length">
        <view class="detail__tag" v-for="tag in post.tags" :key="tag">
          <text class="detail__tag-text">#{{ tag }}</text>
        </view>
      </view>

      <!-- 帖子统计 -->
      <view class="detail__stats">
        <view class="detail__stat">
          <text class="detail__stat-icon">👁️</text>
          <text class="detail__stat-text">{{ post.viewCount || 0 }} 浏览</text>
        </view>
        <view class="detail__stat" v-if="post.hasAnswer">
          <text class="detail__stat-icon">💬</text>
          <text class="detail__stat-text">1 回答</text>
        </view>
        <view class="detail__stat" v-else>
          <text class="detail__stat-icon">💬</text>
          <text class="detail__stat-text">{{ post.commentCount || 0 }} 评论</text>
        </view>
        <view class="detail__stat">
          <text class="detail__stat-icon">👍</text>
          <text class="detail__stat-text">{{ post.likeCount || 0 }} 点赞</text>
        </view>
      </view>
    </view>

    <!-- 回答列表：仅一对一提问被学长学姐回答后展示，论坛帖不显示该区域（讨论走评论区） -->
    <view class="detail__answers" v-if="answers.length > 0">
      <view class="detail__answers-header">
        <text class="detail__answers-title">学长学姐的回答</text>
      </view>

      <view class="detail__answer-list">
        <view class="detail__answer-card" v-for="answer in answers" :key="answer.id">
          <!-- 回答者信息 -->
          <view class="detail__answer-author">
            <view :class="['detail__answer-avatar', answer.authorRole === 'volunteer' && 'detail__answer-avatar--volunteer']">
              <text class="detail__answer-avatar-text">{{ answer.authorName?.charAt(0) || '匿' }}</text>
            </view>
            <view class="detail__answer-author-info">
              <text class="detail__answer-author-name">{{ formatIdentityName(answer.authorName, 'volunteer') }}</text>
              <view class="detail__answer-author-tag" v-if="answer.authorRole === 'volunteer'">
                <text class="detail__answer-author-tag-text">{{ answer.authorUniversity || '大学生' }}</text>
              </view>
            </view>
            <text class="detail__answer-time">{{ formatTime(answer.createdAt) }}</text>
          </view>

          <!-- 回答内容 -->
          <text class="detail__answer-content">{{ answer.content }}</text>

          <!-- 回答操作 -->
          <view class="detail__answer-actions">
            <view class="detail__answer-action" @tap="likeAnswer(answer)" :class="{ 'detail__answer-action--active': answer.liked }">
              <text class="detail__answer-action-icon">👍</text>
              <text class="detail__answer-action-text">{{ answer.likeCount || 0 }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 评论列表 -->
    <view class="detail__comments" v-if="comments.length > 0">
      <view class="detail__comments-header">
        <text class="detail__comments-title">全部评论 ({{ comments.length }})</text>
      </view>

      <view class="detail__comment-list">
        <view class="detail__comment-card" v-for="comment in comments" :key="comment.id">
          <!-- 评论者信息 -->
          <view class="detail__comment-author">
            <view :class="['detail__comment-avatar', comment.userType === 'volunteer' && 'detail__comment-avatar--volunteer']">
              <text class="detail__comment-avatar-text">{{ comment.userName?.charAt(0) || '匿' }}</text>
            </view>
            <view class="detail__comment-author-info">
              <text class="detail__comment-author-name">{{ formatIdentityName(comment.userName, comment.userType) }}</text>
              <view class="detail__comment-author-tag" v-if="comment.userType === 'volunteer'">
                <text class="detail__comment-author-tag-text">大学生</text>
              </view>
            </view>
            <text class="detail__comment-time">{{ formatTime(comment.createdAt) }}</text>
          </view>

          <!-- 评论内容 -->
          <text class="detail__comment-content">
            <text v-if="comment.parentUserName" class="detail__comment-reply-to">回复 {{ comment.parentUserName }}：</text>
            {{ comment.content }}
          </text>

          <!-- 评论操作 -->
          <view class="detail__comment-actions">
            <view class="detail__comment-action" @tap="showReplyInput(comment)">
              <text class="detail__comment-action-icon">💬</text>
              <text class="detail__comment-action-text">回复</text>
            </view>
            <view class="detail__comment-action" @tap="reportComment(comment)">
              <text class="detail__comment-action-icon">🚩</text>
              <text class="detail__comment-action-text">举报</text>
            </view>
          </view>

          <!-- 子评论 -->
          <view class="detail__comment-children" v-if="comment.children && comment.children.length > 0">
            <view class="detail__comment-child" v-for="child in comment.children" :key="child.id">
              <view class="detail__comment-child-author">
                <text class="detail__comment-child-name">{{ formatIdentityName(child.userName, child.userType) }}</text>
                <text class="detail__comment-child-time">{{ formatTime(child.createdAt) }}</text>
              </view>
              <text class="detail__comment-child-content">
                <text v-if="child.parentUserName" class="detail__comment-reply-to">回复 {{ child.parentUserName }}：</text>
                {{ child.content }}
              </text>
              <view class="detail__comment-child-actions">
                <text class="detail__comment-child-action" @tap="reportComment(child)">举报</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="detail__footer">
      <view class="detail__input-wrap" @tap="showReplyInput()">
        <text class="detail__input-placeholder">写评论...</text>
      </view>
      <view class="detail__footer-actions">
        <view class="detail__footer-action" @tap="likePost" :class="{ 'detail__footer-action--active': post?.liked }">
          <text class="detail__footer-action-icon">👍</text>
          <text class="detail__footer-action-text">{{ post?.likeCount || 0 }}</text>
        </view>
        <view class="detail__footer-action" @tap="favoritePost" :class="{ 'detail__footer-action--active': post?.favorited }">
          <text class="detail__footer-action-icon">{{ post?.favorited ? '⭐' : '☆' }}</text>
          <text class="detail__footer-action-text">{{ post?.favorited ? '已收藏' : '收藏' }}</text>
        </view>
        <view class="detail__footer-action" @tap="sharePost">
          <text class="detail__footer-action-icon">📤</text>
          <text class="detail__footer-action-text">分享</text>
        </view>
      </view>
    </view>

    <!-- 回复弹窗 -->
    <view class="detail__reply-mask" v-if="showReply" @tap="hideReplyInput">
      <view class="detail__reply-popup" @tap.stop>
        <view class="detail__reply-header">
          <text class="detail__reply-title">{{ replyToComment ? '回复评论' : '写评论' }}</text>
          <view class="detail__reply-close" @tap="hideReplyInput">
            <text class="detail__reply-close-icon">✕</text>
          </view>
        </view>
        <textarea
          class="detail__reply-input"
          v-model="replyContent"
          :placeholder="replyToComment ? `回复 ${replyToComment.userName}...` : '分享你的观点或经验...'"
          :maxlength="2000"
          :auto-height="true"
        />
        <view class="detail__reply-footer">
          <text class="detail__reply-count">{{ replyContent.length }}/2000</text>
          <view class="detail__reply-submit" @tap="submitReply">
            <text class="detail__reply-submit-text">发布</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { formatTime, formatIdentityName } from '@/utils/format'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import { storeToRefs } from 'pinia'
import * as qaApi from '@/api/qa'
import * as commentApi from '@/api/comment'
import * as forumApi from '@/api/forum'

const userStore = useUserStore()
const appStore = useAppStore()
const { menuButtonWidth } = storeToRefs(appStore)
const post = ref(null)
const answers = ref([])
const comments = ref([])
const showReply = ref(false)
const replyContent = ref('')
const currentPostId = ref(null)
const replyToComment = ref(null)
const submittingReply = ref(false)

onLoad((options) => {
  if (options.id && options.id !== 'undefined') {
    currentPostId.value = options.id
    loadPost(options.id)
    loadComments(options.id)
  }
})

async function loadPost(id) {
  try {
    const detail = await qaApi.getQADetail(id)
    const isStudent = detail.authorType ? detail.authorType === 'student' : !!detail.askerHighSchool
    post.value = {
      id: detail.id,
      title: detail.title || '',
      content: detail.questionContent || '',
      authorName: detail.askerName || '匿名',
      authorSchool: detail.askerHighSchool || detail.universityName || detail.answererUniversity || '',
      authorUniversity: detail.universityName || detail.answererUniversity || '',
      authorRole: isStudent ? 'student' : 'volunteer',
      viewCount: detail.viewCount || 0,
      hasAnswer: !!detail.answerContent,
      commentCount: detail.commentCount || 0,
      likeCount: detail.likeCount || 0,
      liked: !!detail.liked,
      favoriteCount: detail.favoriteCount || 0,
      favorited: !!detail.favorited,
      tags: [],
      isFeatured: !!detail.isFeatured,
      createdAt: detail.askedAt
    }
    if (detail.answerContent) {
      answers.value = [{
        id: 1,
        content: detail.answerContent,
        authorName: detail.answererName || '学长/学姐',
        authorUniversity: detail.answererUniversity || '',
        authorRole: 'volunteer',
        likeCount: detail.answerLikeCount || 0,
        liked: !!detail.answerLiked,
        createdAt: detail.answeredAt
      }]
    } else {
      answers.value = []
    }
  } catch (e) {
    console.error('加载详情失败:', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

const REPORT_REASONS = ['违法违规', '辱骂攻击', '广告引流', '虚假信息', '其他']

function reportPost() {
  if (!ensureIdentity() || !post.value) return
  openReport({ postId: post.value.id, targetType: 'post' }, '举报帖子')
}

function reportComment(comment) {
  if (!ensureIdentity() || !post.value) return
  openReport({ postId: post.value.id, targetType: 'comment', commentId: comment.id }, '举报评论')
}

function openReport(payload, title) {
  uni.showActionSheet({ itemList: REPORT_REASONS, success: (res) => {
    const reason = REPORT_REASONS[res.tapIndex]
    uni.showModal({ title, editable: true, placeholderText: '补充说明（选填）',
      success: async (modal) => {
        if (!modal.confirm) return
        try {
          await forumApi.reportPost({ ...payload, reason, detail: (modal.content || '').trim() })
          uni.showToast({ title: '举报已提交', icon: 'success' })
        } catch (e) { uni.showToast({ title: e.message || '提交失败', icon: 'none' }) }
      }
    })
  } })
}

function goBack() {
  uni.navigateBack()
}

async function loadComments(postId) {
  try {
    const res = await commentApi.getComments(postId)
    comments.value = res || []
  } catch (e) {
    console.error('加载评论失败:', e)
    comments.value = []
  }
}

function showReplyInput(comment = null) {
  // 评论需绑定身份：未登录跳登录页，未完善资料引导完善（避免提交后才报错）
  if (!ensureIdentity()) return
  replyToComment.value = comment
  showReply.value = true
}

function hideReplyInput() {
  showReply.value = false
  replyToComment.value = null
}

async function submitReply() {
  // 防重复提交：提交中禁止再次触发（快速连点会创建重复评论/重复通知）
  if (submittingReply.value) return
  if (!replyContent.value.trim()) {
    uni.showToast({ title: '请输入内容', icon: 'none' })
    return
  }

  if (!currentPostId.value) {
    uni.showToast({ title: '帖子信息异常', icon: 'none' })
    return
  }

  submittingReply.value = true
  uni.showLoading({ title: '提交中...' })

  try {
    // 提交评论（回答功能由志愿者在"回答问题"页完成，此处只有评论入口）
    await commentApi.addComment({
      postId: parseInt(currentPostId.value),
      content: replyContent.value.trim(),
      parentId: replyToComment.value?.id || null
    })
    // 重新加载评论
    await loadComments(currentPostId.value)

    uni.hideLoading()

    replyContent.value = ''
    showReply.value = false
    replyToComment.value = null

    uni.showToast({ title: '发布成功', icon: 'success' })
  } catch (e) {
    uni.hideLoading()
    console.error('提交失败:', e)
    uni.showToast({ title: e.message || '提交失败，请重试', icon: 'none' })
  } finally {
    submittingReply.value = false
  }
}

// 点赞/收藏前校验身份：未绑定学生/志愿者身份时引导完善资料
function ensureIdentity() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/common/login' })
    return false
  }
  if (!userStore.hasBoundIdentity) {
    uni.showModal({
      title: '提示',
      content: '请先完善个人资料后再点赞收藏',
      confirmText: '去完善',
      success: (res) => {
        if (res.confirm) {
          const url = userStore.isVolunteer
            ? '/pages/volunteer/register'
            : '/pages/common/edit-profile?register=1'
          uni.navigateTo({ url })
        }
      }
    })
    return false
  }
  return true
}

// 帖子点赞切换
async function likePost() {
  if (!ensureIdentity() || !post.value) return
  try {
    const res = await qaApi.toggleLike(post.value.id)
    post.value.liked = !!res.liked
    post.value.likeCount = res.count || 0
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败，请重试', icon: 'none' })
  }
}

// 回答点赞切换
async function likeAnswer(answer) {
  if (!ensureIdentity() || !post.value) return
  try {
    const res = await qaApi.toggleAnswerLike(post.value.id)
    answer.liked = !!res.liked
    answer.likeCount = res.count || 0
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败，请重试', icon: 'none' })
  }
}

// 收藏切换
async function favoritePost() {
  if (!ensureIdentity() || !post.value) return
  try {
    const res = await qaApi.toggleFavorite(post.value.id)
    post.value.favorited = !!res.favorited
    post.value.favoriteCount = res.count || 0
    uni.showToast({ title: res.favorited ? '收藏成功' : '已取消收藏', icon: 'none' })
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败，请重试', icon: 'none' })
  }
}

function sharePost() {
  // TODO: 实现分享功能
  uni.showToast({ title: '分享功能开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.detail {
  min-height: 100vh;
  background: #F2F6FB;
  padding-bottom: 140rpx;

  // 导航栏（custom 模式需适配状态栏高度，否则标题栏会顶到状态栏下方）
  &__navbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: calc(88rpx + var(--status-bar-height));
    padding: var(--status-bar-height) 24rpx 0;
    box-sizing: border-box;
    background: #fff;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
    position: sticky;
    top: 0;
    z-index: 10;
  }

  &__back {
    width: 60rpx;
    height: 60rpx;
    display: flex;
    align-items: center;
    justify-content: center;

    &-icon {
      font-size: 48rpx;
      color: #374151;
      font-weight: 300;
    }
  }

  &__navbar-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #1f2937;
  }

  &__navbar-right {
    width: 60rpx;
    font-size: 22rpx;
    color: #64748b;
    text-align: right;
  }

  // 帖子内容
  &__post {
    background: #fff;
    margin: 24rpx;
    border-radius: 24rpx;
    padding: 28rpx;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  }

  &__author {
    display: flex;
    align-items: center;
    margin-bottom: 24rpx;
  }

  &__avatar {
    width: 72rpx;
    height: 72rpx;
    background: linear-gradient(135deg, #fbbf24, #f59e0b);
    border-radius: 24rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16rpx;

    &--volunteer {
      background: linear-gradient(135deg, #667eea, #764ba2);
    }

    &-text {
      font-size: 32rpx;
      font-weight: 600;
      color: #fff;
    }
  }

  &__author-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4rpx;
  }

  &__author-name {
    font-size: 28rpx;
    font-weight: 600;
    color: #1f2937;
  }

  &__author-tag {
    display: inline-flex;
    background: #ede9fe;
    padding: 2rpx 12rpx;
    border-radius: 6rpx;
    align-self: flex-start;

    &--student {
      background: #fef3c7;
    }

    &-text {
      font-size: 20rpx;
      color: #5B9BD5;
    }

    &--student &-text {
      color: #d97706;
    }
  }

  &__post-time {
    font-size: 24rpx;
    color: #9ca3af;
  }

  &__title-row {
    display: flex;
    align-items: flex-start;
    gap: 12rpx;
    margin-bottom: 20rpx;
  }

  &__featured {
    flex-shrink: 0;
    padding: 5rpx 10rpx;
    color: #9a3412;
    background: #ffedd5;
    font-size: 22rpx;
    line-height: 1.4;
  }

  &__title {
    flex: 1;
    min-width: 0;
    font-size: 36rpx;
    font-weight: 700;
    color: #1f2937;
    display: block;
    margin-bottom: 0;
    line-height: 1.4;
  }

  &__content {
    font-size: 28rpx;
    color: #374151;
    line-height: 1.8;
    display: block;
    margin-bottom: 20rpx;
    white-space: pre-wrap;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8rpx;
    margin-bottom: 20rpx;
  }

  &__tag {
    background: #f3f4f6;
    padding: 6rpx 16rpx;
    border-radius: 8rpx;

    &-text {
      font-size: 22rpx;
      color: #5B9BD5;
    }
  }

  &__stats {
    display: flex;
    gap: 32rpx;
    padding-top: 20rpx;
    border-top: 2rpx solid #f3f4f6;
  }

  &__stat {
    display: flex;
    align-items: center;
    gap: 8rpx;

    &-icon {
      font-size: 24rpx;
    }

    &-text {
      font-size: 24rpx;
      color: #9ca3af;
    }
  }

  // 回答列表
  &__answers {
    margin: 0 24rpx;
  }

  &__answers-header {
    margin-bottom: 20rpx;
  }

  &__answers-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #1f2937;
  }

  &__answer-list {
    display: flex;
    flex-direction: column;
    gap: 16rpx;
  }

  &__answer-card {
    background: #fff;
    border-radius: 24rpx;
    padding: 24rpx;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  }

  &__answer-author {
    display: flex;
    align-items: center;
    margin-bottom: 16rpx;
  }

  &__answer-avatar {
    width: 56rpx;
    height: 56rpx;
    background: linear-gradient(135deg, #fbbf24, #f59e0b);
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12rpx;

    &--volunteer {
      background: linear-gradient(135deg, #667eea, #764ba2);
    }

    &-text {
      font-size: 24rpx;
      font-weight: 600;
      color: #fff;
    }
  }

  &__answer-author-info {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8rpx;
  }

  &__answer-author-name {
    font-size: 26rpx;
    font-weight: 600;
    color: #1f2937;
  }

  &__answer-author-tag {
    background: #ede9fe;
    padding: 2rpx 10rpx;
    border-radius: 4rpx;

    &-text {
      font-size: 18rpx;
      color: #5B9BD5;
    }
  }

  &__answer-time {
    font-size: 22rpx;
    color: #9ca3af;
  }

  &__answer-content {
    font-size: 28rpx;
    color: #374151;
    line-height: 1.7;
    display: block;
    margin-bottom: 16rpx;
    white-space: pre-wrap;
  }

  &__answer-actions {
    display: flex;
    gap: 32rpx;
    padding-top: 16rpx;
    border-top: 2rpx solid #f3f4f6;
  }

  &__answer-action {
    display: flex;
    align-items: center;
    gap: 8rpx;

    &--active &-icon {
      transform: scale(1.1);
    }

    &--active &-text {
      color: #ff4d4f;
      font-weight: 600;
    }

    &-icon {
      font-size: 24rpx;
    }

    &-text {
      font-size: 22rpx;
      color: #9ca3af;
    }
  }

  // 评论列表
  &__comments {
    margin: 24rpx;
  }

  &__comments-header {
    margin-bottom: 20rpx;
  }

  &__comments-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #1f2937;
  }

  &__comment-list {
    display: flex;
    flex-direction: column;
    gap: 16rpx;
  }

  &__comment-card {
    background: #fff;
    border-radius: 24rpx;
    padding: 24rpx;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  }

  &__comment-author {
    display: flex;
    align-items: center;
    margin-bottom: 12rpx;
  }

  &__comment-avatar {
    width: 48rpx;
    height: 48rpx;
    background: linear-gradient(135deg, #fbbf24, #f59e0b);
    border-radius: 12rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12rpx;

    &--volunteer {
      background: linear-gradient(135deg, #667eea, #764ba2);
    }

    &-text {
      font-size: 20rpx;
      font-weight: 600;
      color: #fff;
    }
  }

  &__comment-author-info {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8rpx;
  }

  &__comment-author-name {
    font-size: 24rpx;
    font-weight: 600;
    color: #1f2937;
  }

  &__comment-author-tag {
    background: #ede9fe;
    padding: 2rpx 8rpx;
    border-radius: 4rpx;

    &-text {
      font-size: 16rpx;
      color: #5B9BD5;
    }
  }

  &__comment-time {
    font-size: 20rpx;
    color: #9ca3af;
  }

  &__comment-content {
    font-size: 26rpx;
    color: #374151;
    line-height: 1.6;
    display: block;
    margin-bottom: 12rpx;
  }

  &__comment-reply-to {
    color: #5B9BD5;
    font-weight: 500;
  }

  &__comment-actions {
    display: flex;
    gap: 32rpx;
    padding-top: 12rpx;
    border-top: 2rpx solid #f3f4f6;
  }

  &__comment-action {
    display: flex;
    align-items: center;
    gap: 8rpx;

    &-icon {
      font-size: 22rpx;
    }

    &-text {
      font-size: 20rpx;
      color: #9ca3af;
    }
  }

  &__comment-children {
    margin-top: 16rpx;
    padding: 16rpx;
    background: #f9fafb;
    border-radius: 16rpx;
  }

  &__comment-child {
    &:not(:last-child) {
      margin-bottom: 12rpx;
      padding-bottom: 12rpx;
      border-bottom: 2rpx solid #f3f4f6;
    }
  }

  &__comment-child-author {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8rpx;
  }

  &__comment-child-name {
    font-size: 22rpx;
    font-weight: 600;
    color: #1f2937;
  }

  &__comment-child-time {
    font-size: 18rpx;
    color: #9ca3af;
  }

  &__comment-child-content {
    font-size: 24rpx;
    color: #374151;
    line-height: 1.5;
  }

  &__comment-child-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 8rpx;
  }

  &__comment-child-action {
    font-size: 20rpx;
    color: #94a3b8;
    padding: 4rpx 12rpx;
  }

  // 空状态
  &__empty {
    background: #fff;
    border-radius: 24rpx;
    padding: 60rpx 24rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);

    &-icon {
      font-size: 64rpx;
      margin-bottom: 16rpx;
    }

    &-text {
      font-size: 28rpx;
      color: #9ca3af;
    }
  }

  // 底部操作栏
  &__footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: #fff;
    padding: 16rpx 24rpx;
    padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
    display: flex;
    align-items: center;
    gap: 24rpx;
    box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
    z-index: 100;
  }

  &__input-wrap {
    flex: 1;
    height: 72rpx;
    background: #f3f4f6;
    border-radius: 36rpx;
    padding: 0 24rpx;
    display: flex;
    align-items: center;
  }

  &__input-placeholder {
    font-size: 26rpx;
    color: #9ca3af;
  }

  &__footer-actions {
    display: flex;
    gap: 24rpx;
  }

  &__footer-action {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2rpx;

    &--active &-icon {
      transform: scale(1.05);
    }

    &--active &-text {
      color: #ff4d4f;
      font-weight: 600;
    }

    &-icon {
      font-size: 36rpx;
    }

    &-text {
      font-size: 18rpx;
      color: #9ca3af;
    }
  }

  // 回复弹窗
  &__reply-mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 1000;
    display: flex;
    align-items: flex-end;
  }

  &__reply-popup {
    width: 100%;
    background: #fff;
    border-radius: 32rpx 32rpx 0 0;
    padding: 32rpx;
    padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
    max-height: 80vh;
  }

  &__reply-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
  }

  &__reply-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #1f2937;
  }

  &__reply-close {
    width: 48rpx;
    height: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;

    &-icon {
      font-size: 32rpx;
      color: #9ca3af;
    }
  }

  &__reply-input {
    width: 100%;
    min-height: 200rpx;
    background: #f3f4f6;
    border-radius: 16rpx;
    padding: 20rpx;
    font-size: 28rpx;
    color: #374151;
    line-height: 1.6;
    margin-bottom: 16rpx;
  }

  &__reply-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__reply-count {
    font-size: 24rpx;
    color: #9ca3af;
  }

  &__reply-submit {
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 24rpx;
    padding: 16rpx 40rpx;

    &-text {
      font-size: 28rpx;
      color: #fff;
      font-weight: 600;
    }
  }
}
</style>
