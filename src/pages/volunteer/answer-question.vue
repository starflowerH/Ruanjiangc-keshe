<template>
  <view class="answer-question">
    <NavBar title="回答问题" />

    <view class="answer-question__content">
      <!-- 问题详情 -->
      <view class="answer-question__question">
        <view class="answer-question__q-header">
          <view class="answer-question__asker-info">
            <image class="answer-question__asker-avatar" :src="question.askerAvatar || '/static/images/default-avatar.png'" mode="aspectFill" />
            <view class="answer-question__asker-detail">
              <text class="answer-question__asker-name">{{ question.askerName }}</text>
              <text class="answer-question__asker-school">{{ question.askerSchool }}</text>
            </view>
          </view>
          <text class="answer-question__time">{{ formatTime(question.createdAt) }}</text>
        </view>
        <text class="answer-question__q-text">{{ question.question }}</text>
      </view>

      <!-- 回答输入 -->
      <view class="answer-question__field">
        <text class="answer-question__label">你的回答</text>
        <textarea
          class="answer-question__textarea"
          v-model="answer"
          placeholder="请输入你的回答，帮助学弟学妹了解更多信息..."
          maxlength="1000"
          :auto-height="false"
        />
        <text class="answer-question__count">{{ answer.length }}/1000</text>
      </view>

      <!-- 快捷回答 -->
      <view class="answer-question__quick">
        <text class="answer-question__quick-title">快捷回答模板</text>
        <view class="answer-question__quick-list">
          <view
            class="answer-question__quick-item"
            v-for="tpl in quickTemplates"
            :key="tpl"
            @tap="useTemplate(tpl)"
          >
            <text class="answer-question__quick-text">{{ tpl }}</text>
          </view>
        </view>
      </view>

      <!-- 提交按钮 -->
      <view class="answer-question__btn" @tap="handleSubmit">
        <text class="answer-question__btn-text">提交回答</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { formatTime } from '@/utils/format'
import NavBar from '@/components/NavBar.vue'
import * as qaApi from '@/api/qa'

const questionId = ref(null)
const answer = ref('')
const submitting = ref(false)

const question = reactive({
  id: null,
  question: '',
  askerName: '',
  askerSchool: '',
  askerAvatar: '',
  createdAt: ''
})

const quickTemplates = [
  '这个问题很好，我来详细解答一下...',
  '根据我的经验...',
  '我们学校在这方面的情况是...'
]

onLoad(async (options) => {
  if (options.id && options.id !== 'undefined') {
    questionId.value = options.id
    await loadQuestion(options.id)
  }
})

async function loadQuestion(id) {
  try {
    const detail = await qaApi.getQADetail(id)
    // 映射 API 字段到页面字段
    Object.assign(question, {
      id: detail.id,
      question: detail.questionContent,
      askerName: detail.askerName,
      askerSchool: detail.askerHighSchool,
      createdAt: detail.askedAt
    })
  } catch (e) {
    console.error('加载问题详情失败:', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

function useTemplate(tpl) {
  answer.value = tpl
}

async function handleSubmit() {
  // 防重复提交：提交中禁止再次触发（快速连点会创建重复回答/通知）
  if (submitting.value) return
  if (!answer.value.trim()) {
    uni.showToast({ title: '请输入回答内容', icon: 'none' })
    return
  }

  submitting.value = true
  uni.showLoading({ title: '提交中...' })

  try {
    await qaApi.answerQuestion({
      questionId: questionId.value,
      answerContent: answer.value.trim()
    })

    uni.hideLoading()
    uni.showToast({ title: '回答成功', icon: 'success' })

    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (e) {
    uni.hideLoading()
    console.error('回答失败:', e)
    uni.showToast({ title: '提交失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.answer-question {
  min-height: 100vh;
  background: #F2F6FB;

  &__content {
    padding: 24rpx;
  }

  &__question {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 24rpx;
  }

  &__q-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16rpx;
  }

  &__asker-info {
    display: flex;
    align-items: center;
  }

  &__asker-avatar {
    width: 56rpx;
    height: 56rpx;
    border-radius: 50%;
    margin-right: 12rpx;
    background: #f0f0f0;
  }

  &__asker-detail {
    display: flex;
    flex-direction: column;
  }

  &__asker-name {
    font-size: 26rpx;
    color: #333;
    font-weight: 500;
  }

  &__asker-school {
    font-size: 22rpx;
    color: #999;
  }

  &__time {
    font-size: 22rpx;
    color: #999;
  }

  &__q-text {
    font-size: 28rpx;
    color: #333;
    line-height: 1.6;
  }

  &__field {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 24rpx;
  }

  &__label {
    font-size: 28rpx;
    font-weight: 500;
    color: #333;
    display: block;
    margin-bottom: 16rpx;
  }

  &__textarea {
    width: 100%;
    min-height: 240rpx;
    font-size: 28rpx;
    color: #333;
    line-height: 1.6;
    padding: 16rpx;
    border: 2rpx solid #e8e8e8;
    border-radius: 12rpx;
    background: #fafafa;
  }

  &__count {
    font-size: 22rpx;
    color: #999;
    text-align: right;
    display: block;
    margin-top: 8rpx;
  }

  &__quick {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 32rpx;
  }

  &__quick-title {
    font-size: 28rpx;
    font-weight: 500;
    color: #333;
    display: block;
    margin-bottom: 16rpx;
  }

  &__quick-list {
    display: flex;
    flex-direction: column;
    gap: 12rpx;
  }

  &__quick-item {
    background: #f0f7ff;
    border-radius: 12rpx;
    padding: 16rpx 20rpx;
  }

  &__quick-text {
    font-size: 24rpx;
    color: #5B9BD5;
  }

  &__btn {
    width: 100%;
    height: 96rpx;
    background: linear-gradient(135deg, #52c41a, #389e0d);
    border-radius: 12rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__btn-text {
    font-size: 32rpx;
    color: #fff;
    font-weight: 600;
  }
}
</style>
