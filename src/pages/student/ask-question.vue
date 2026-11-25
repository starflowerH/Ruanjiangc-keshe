<template>
  <view class="ask">
    <!-- 导航栏 -->
    <view class="ask__navbar">
      <view class="ask__back" @tap="goBack">
        <text class="ask__back-icon">‹</text>
      </view>
      <text class="ask__navbar-title">{{ isForumMode ? '发布帖子' : '我要提问' }}</text>
      <view class="ask__navbar-right"></view>
    </view>

    <view class="ask__content">
      <!-- 论坛模式提示 -->
      <view class="ask__tip" v-if="isForumMode">
        <text class="ask__tip-icon">💡</text>
        <text class="ask__tip-text">发布后所有人可见，欢迎在评论区讨论</text>
      </view>

      <!-- 指定志愿者提问 -->
      <view class="ask__target" v-if="target.id">
        <view class="ask__target-avatar">
          <text class="ask__target-avatar-text">{{ target.name?.charAt(0) || '学' }}</text>
        </view>
        <view class="ask__target-info">
          <text class="ask__target-name">{{ target.name }}</text>
          <text class="ask__target-school">{{ target.universityName }}</text>
        </view>
      </view>

      <!-- 标签选择（论坛模式） -->
      <view class="ask__tags" v-if="isForumMode">
        <text class="ask__label">选择标签</text>
        <view class="ask__tag-list">
          <view
            v-for="tag in tagOptions"
            :key="tag.value"
            :class="['ask__tag', selectedTag === tag.value && 'ask__tag--active']"
            @tap="selectTag(tag.value)"
          >
            <text class="ask__tag-text">{{ tag.label }}</text>
          </view>
        </view>
      </view>

      <view class="ask__field" v-if="isForumMode && userStore.isStudent">
        <text class="ask__label">关联大学</text>
        <UniversitySearch
          :selected-id="selectedUniversity?.id"
          :selected-name="selectedUniversity?.name"
          placeholder="请选择关联大学"
          @select="onUniversitySelect"
        />
      </view>

      <!-- 问题输入 -->
      <view class="ask__field">
        <text class="ask__label">{{ isForumMode ? '详细内容' : '你的问题' }}</text>
        <textarea
          class="ask__textarea"
          v-model="content"
          :placeholder="isForumMode ? '详细描述你想了解的内容...' : '请输入你想问的问题，例如：贵校的XX专业怎么样？'"
          maxlength="2000"
          :auto-height="false"
        />
        <text class="ask__count">{{ content.length }}/2000</text>
      </view>

      <!-- 快捷问题 -->
      <view class="ask__quick" v-if="!isForumMode">
        <text class="ask__quick-title">常见问题</text>
        <view class="ask__quick-list">
          <view
            class="ask__quick-item"
            v-for="q in quickQuestions"
            :key="q"
            @tap="useQuickQuestion(q)"
          >
            <text class="ask__quick-text">{{ q }}</text>
          </view>
        </view>
      </view>

      <!-- 提交按钮 -->
      <view class="ask__btn" @tap="handleSubmit" hover-class="ask__btn--hover">
        <text class="ask__btn-text">{{ isForumMode ? '发布帖子' : '提交问题' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import * as qaApi from '@/api/qa'
import UniversitySearch from '@/components/UniversitySearch.vue'

const userStore = useUserStore()

const target = reactive({
  id: null,
  name: '',
  universityName: ''
})

const content = ref('')
const selectedTag = ref('other')
const isForumMode = ref(false)
const selectedUniversity = ref(null)
const submitting = ref(false)

const tagOptions = [
  { label: '报考咨询', value: 'admission' },
  { label: '专业选择', value: 'major' },
  { label: '校园生活', value: 'campus' },
  { label: '学习方法', value: 'study' },
  { label: '其他', value: 'other' }
]

const quickQuestions = [
  '贵校的王牌专业是什么？',
  '校园环境和宿舍条件怎么样？',
  '毕业后的就业情况如何？',
  '有什么社团活动推荐？',
  '转专业容易吗？'
]

onLoad(async (options) => {
  // 判断是否为论坛模式
  if (options.mode === 'forum') {
    isForumMode.value = true
  }

  // 指定志愿者提问
  if (options.volunteerId && options.volunteerId !== 'undefined') {
    target.id = options.volunteerId
    target.name = decodeURIComponent(options.volunteerName || '')
    target.universityName = decodeURIComponent(options.universityName || '')
  }
})

function onUniversitySelect(university) { selectedUniversity.value = university }

function selectTag(tag) {
  selectedTag.value = tag
}

function useQuickQuestion(q) {
  content.value = q
}

function goBack() {
  uni.navigateBack()
}

async function handleSubmit() {
  // 防重复提交：提交中禁止再次触发（快速连点会创建重复帖子/重复通知）
  if (submitting.value) return
  // 防御：提交前检查是否已绑定身份
  if (!userStore.hasBoundIdentity) {
    uni.showModal({
      title: '提示',
      content: '请先完善个人资料后再提问',
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
    return
  }

  if (!content.value.trim()) {
    uni.showToast({ title: isForumMode.value ? '请输入内容' : '请输入问题', icon: 'none' })
    return
  }

  if (isForumMode.value && userStore.isStudent && !selectedUniversity.value) {
    uni.showToast({ title: '请选择帖子关联大学', icon: 'none' })
    return
  }

  submitting.value = true
  uni.showLoading({ title: '提交中...' })

  try {
    await qaApi.submitQuestion({
      questionContent: content.value.trim(),
      title: isForumMode.value ? content.value.trim().substring(0, 20) : undefined,
      category: isForumMode.value ? selectedTag.value : undefined,
      universityId: isForumMode.value ? selectedUniversity.value?.id : undefined,
      targetVolunteerId: target.id || undefined
    })

    uni.hideLoading()
    uni.showToast({ title: isForumMode.value ? '已提交审核' : '发布成功', icon: 'success' })

    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (e) {
    uni.hideLoading()
    console.error('提交失败:', e)
    uni.showToast({ title: e.message || '提交失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.ask {
  min-height: 100vh;
  background: #F2F6FB;

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
  }

  // 内容区域
  &__content {
    padding: 24rpx;
  }

  // 提示
  &__tip {
    display: flex;
    align-items: center;
    gap: 12rpx;
    background: linear-gradient(135deg, #ede9fe, #e0e7ff);
    border-radius: 16rpx;
    padding: 20rpx 24rpx;
    margin-bottom: 24rpx;

    &-icon {
      font-size: 28rpx;
    }

    &-text {
      font-size: 24rpx;
      color: #5B9BD5;
    }
  }

  // 目标志愿者
  &__target {
    background: #fff;
    border-radius: 20rpx;
    padding: 24rpx;
    display: flex;
    align-items: center;
    gap: 16rpx;
    margin-bottom: 24rpx;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);

    &-avatar {
      width: 72rpx;
      height: 72rpx;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;

      &-text {
        font-size: 32rpx;
        font-weight: 600;
        color: #fff;
      }
    }

    &-info {
      display: flex;
      flex-direction: column;
      gap: 4rpx;
    }

    &-name {
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2937;
    }

    &-school {
      font-size: 22rpx;
      color: #6b7280;
    }
  }

  // 标签选择
  &__tags {
    margin-bottom: 24rpx;
  }

  &__label {
    font-size: 26rpx;
    font-weight: 600;
    color: #374151;
    display: block;
    margin-bottom: 16rpx;
  }

  &__tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
  }

  &__tag {
    padding: 12rpx 24rpx;
    background: #fff;
    border-radius: 24rpx;
    border: 2rpx solid #e5e7eb;
    transition: all 0.3s ease;

    &--active {
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-color: transparent;
      box-shadow: 0 4rpx 12rpx rgba(99, 102, 241, 0.3);
    }

    &-text {
      font-size: 24rpx;
      color: #6b7280;
    }

    &--active &-text {
      color: #fff;
    }
  }

  // 输入框
  &__field {
    margin-bottom: 24rpx;
  }

  &__input {
    width: 100%;
    height: 88rpx;
    background: #fff;
    border-radius: 16rpx;
    padding: 0 24rpx;
    font-size: 28rpx;
    color: #1f2937;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  }

  &__textarea {
    width: 100%;
    height: 300rpx;
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    font-size: 28rpx;
    color: #1f2937;
    line-height: 1.6;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  }

  &__count {
    font-size: 22rpx;
    color: #9ca3af;
    display: block;
    text-align: right;
    margin-top: 8rpx;
  }

  &__picker { height: 88rpx; padding: 0 24rpx; display: flex; align-items: center; justify-content: space-between; background: #fff; border: 2rpx solid #e5e7eb; border-radius: 12rpx; font-size: 26rpx; color: #374151; }

  // 快捷问题
  &__quick {
    margin-bottom: 32rpx;

    &-title {
      font-size: 26rpx;
      font-weight: 600;
      color: #374151;
      display: block;
      margin-bottom: 16rpx;
    }

    &-list {
      display: flex;
      flex-direction: column;
      gap: 12rpx;
    }

    &-item {
      background: #fff;
      border-radius: 12rpx;
      padding: 20rpx 24rpx;
      box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
    }

    &-text {
      font-size: 26rpx;
      color: #5B9BD5;
    }
  }

  // 提交按钮
  &__btn {
    width: 100%;
    height: 96rpx;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8rpx 24rpx rgba(99, 102, 241, 0.35);
    margin-top: 40rpx;

    &--hover {
      transform: scale(0.98);
      box-shadow: 0 4rpx 12rpx rgba(99, 102, 241, 0.25);
    }

    &-text {
      font-size: 32rpx;
      color: #fff;
      font-weight: 600;
      letter-spacing: 4rpx;
    }
  }
}
</style>
