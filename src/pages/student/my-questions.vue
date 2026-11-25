<template>
  <view class="my-questions">
    <NavBar title="我的提问" />

    <view class="my-questions__tabs">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        :class="['my-questions__tab', activeTab === tab.key && 'my-questions__tab--active']"
        @tap="activeTab = tab.key"
      >
        <text class="my-questions__tab-text">{{ tab.label }}</text>
      </view>
    </view>

    <view class="my-questions__list">
      <QuestionItem
        v-for="qa in filteredList"
        :key="qa.id"
        :data="qa"
        @tap="goDetail(qa.id)"
      />

      <Empty v-if="!filteredList.length" :text="emptyText" icon="❓" />
    </view>

    <!-- 自定义底部导航栏 -->
    <TabBar currentPath="/pages/student/my-questions" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import QuestionItem from '@/components/QuestionItem.vue'
import Empty from '@/components/Empty.vue'
import TabBar from '@/components/TabBar.vue'
import * as qaApi from '@/api/qa'

const activeTab = ref('all')
const questions = ref([])

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待回答' },
  { key: 'answered', label: '已回答' }
]

const filteredList = computed(() => {
  if (activeTab.value === 'all') return questions.value
  if (activeTab.value === 'pending') {
    return questions.value.filter(q => !q.answerContent)
  }
  if (activeTab.value === 'answered') {
    return questions.value.filter(q => !!q.answerContent)
  }
  return questions.value
})

const emptyText = computed(() => {
  const map = {
    all: '暂无提问记录',
    pending: '没有待回答的问题',
    answered: '没有已回答的问题'
  }
  return map[activeTab.value]
})

onShow(() => {
  loadQuestions()
})

async function loadQuestions() {
  try {
    const res = await qaApi.getMyQuestions()
    // 后端返回分页格式 { records: [...] } 或直接返回数组
    questions.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) {
    console.error('加载提问列表失败:', e)
    questions.value = []
  }
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/student/qa-detail?id=${id}` })
}
</script>

<style lang="scss" scoped>
.my-questions {
  min-height: 100vh;
  background: #F2F6FB;
  padding-bottom: 120rpx;

  &__tabs {
    display: flex;
    background: #fff;
    padding: 0 16rpx;
    border-bottom: 2rpx solid #f0f0f0;
  }

  &__tab {
    flex: 1;
    padding: 24rpx 0;
    text-align: center;
    position: relative;

    &--active {
      .my-questions__tab-text {
        color: #5B9BD5;
        font-weight: 600;
      }

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 4rpx;
        background: #5B9BD5;
        border-radius: 999rpx;
      }
    }
  }

  &__tab-text {
    font-size: 28rpx;
    color: #666;
  }

  &__list {
    padding: 24rpx;
  }
}
</style>
