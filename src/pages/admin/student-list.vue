<template>
  <view class="student-list">
    <NavBar title="高中生列表" show-back />
    <!-- 高中分类导航条（参照待审核页导航按钮） -->
    <scroll-view class="student-list__nav" scroll-x :show-scrollbar="false">
      <view class="student-list__nav-inner">
        <view
          class="student-list__nav-item"
          :class="{ 'student-list__nav-item--active': currentHighSchoolId === null }"
          @tap="selectHighSchool(null)"
        >
          <text class="student-list__nav-text">全部高中</text>
        </view>
        <view
          class="student-list__nav-item"
          :class="{ 'student-list__nav-item--active': currentHighSchoolId === hs.id }"
          v-for="hs in highSchools"
          :key="hs.id"
          @tap="selectHighSchool(hs.id)"
        >
          <text class="student-list__nav-text">{{ hs.name }}</text>
        </view>
      </view>
    </scroll-view>
    <view class="student-list__body">
      <!-- 搜索栏 -->
      <view class="student-list__search">
        <input
          class="student-list__search-input"
          v-model="keyword"
          placeholder="搜索姓名 / 昵称 / 手机号"
          confirm-type="search"
          @confirm="onSearch"
        />
      </view>

      <!-- 统计 -->
      <view class="student-list__stat" v-if="total > 0">
        <text class="student-list__stat-text">共 {{ total }} 名高中生</text>
      </view>

      <!-- 加载中 -->
      <view class="student-list__loading" v-if="loading">
        <text class="student-list__loading-text">加载中...</text>
      </view>

      <!-- 空状态 -->
      <view class="student-list__empty" v-if="!loading && !list.length">
        <text class="student-list__empty-text">暂无高中生数据</text>
      </view>

      <!-- 高中生列表 -->
      <view class="student-list__card" v-for="item in list" :key="item.id">
        <view class="student-list__avatar">
          <image
            v-if="item.avatarUrl"
            class="student-list__avatar-img"
            :src="item.avatarUrl"
            mode="aspectFill"
          />
          <text v-else class="student-list__avatar-text">{{ (item.name || '?')[0] }}</text>
        </view>
        <view class="student-list__info">
          <view class="student-list__name-row">
            <text class="student-list__name">{{ item.name }}</text>
            <text class="student-list__nickname" v-if="item.nickname && item.nickname !== item.name">{{ item.nickname }}</text>
          </view>
          <text class="student-list__phone">📱 {{ item.phone || '未填写' }}</text>
          <view class="student-list__meta">
            <text class="student-list__meta-item">🏫 {{ item.highSchoolName || '未选择' }}</text>
          </view>
        </view>
        <view class="student-list__time">
          <text class="student-list__time-text">{{ formatTime(item.createdAt) }}</text>
        </view>
      </view>

      <!-- 加载更多 -->
      <view class="student-list__more" v-if="hasMore">
        <text class="student-list__more-text" @tap="loadMore">加载更多</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import { getStudentList, getHighSchoolList } from '@/api/admin'

const loading = ref(false)
const list = ref([])
const keyword = ref('')
const page = ref(1)
const size = 20
const total = ref(0)
const hasMore = ref(false)
const highSchools = ref([])
const currentHighSchoolId = ref(null)

onShow(() => {
  loadHighSchools()
  page.value = 1
  loadStudents(true)
})

// 加载高中列表（用于导航按钮）
async function loadHighSchools() {
  try {
    const res = await getHighSchoolList()
    highSchools.value = res || []
  } catch (e) {
    console.error('加载高中列表失败:', e)
  }
}

// 选择高中（筛选）
function selectHighSchool(id) {
  currentHighSchoolId.value = id
  page.value = 1
  loadStudents(true)
}

async function loadStudents(reset) {
  if (loading.value) return
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (currentHighSchoolId.value !== null) {
      params.highSchoolId = currentHighSchoolId.value
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim()
    }
    const res = await getStudentList(params)
    const records = res?.records || []
    total.value = res?.total || 0
    if (reset) {
      list.value = records
    } else {
      list.value = [...list.value, ...records]
    }
    hasMore.value = list.value.length < total.value
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  loadStudents(true)
}

function loadMore() {
  page.value += 1
  loadStudents(false)
}

function formatTime(t) {
  if (!t) return ''
  const s = String(t)
  return s.slice(0, 10)
}
</script>

<style lang="scss" scoped>
.student-list {
  min-height: 100vh;
  background: #f2f6fb;
  padding-bottom: 40rpx;

  // 高中分类导航条（参照待审核页导航按钮）
  &__nav {
    background: #fff;
    white-space: nowrap;
    box-shadow: 0 4rpx 16rpx rgba(76, 122, 168, 0.06);
  }

  &__nav-inner {
    display: inline-flex;
    padding: 0 24rpx;
    height: 92rpx;
    align-items: center;
  }

  &__nav-item {
    padding: 14rpx 30rpx;
    margin-right: 16rpx;
    border-radius: 999rpx;
    background: #f4f8fc;
    flex-shrink: 0;

    &--active {
      background: linear-gradient(135deg, #6aa6dd, #4a86c4);
    }
  }

  &__nav-text {
    font-size: 26rpx;
    color: #5b708a;
  }

  &__nav-item--active &__nav-text {
    color: #fff;
    font-weight: 600;
  }

  &__body {
    padding: 24rpx 28rpx;
  }

  &__search {
    background: #fff;
    border-radius: 20rpx;
    padding: 8rpx 24rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 4rpx 16rpx rgba(76, 122, 168, 0.06);
  }

  &__search-input {
    height: 76rpx;
    font-size: 28rpx;
    color: #33445c;
  }

  &__stat {
    margin-bottom: 20rpx;
    padding-left: 4rpx;
  }

  &__stat-text {
    font-size: 26rpx;
    color: #8aa0b8;
  }

  &__card {
    display: flex;
    align-items: center;
    background: #fff;
    border-radius: 24rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 6rpx 24rpx rgba(76, 122, 168, 0.08);
  }

  &__avatar {
    width: 88rpx;
    height: 88rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #7fb7e6, #5a9ad0);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
    flex-shrink: 0;
    overflow: hidden;
  }

  &__avatar-img {
    width: 100%;
    height: 100%;
  }

  &__avatar-text {
    font-size: 36rpx;
    font-weight: 700;
    color: #fff;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name-row {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-bottom: 8rpx;
    min-width: 0;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 700;
    color: #2b3f5a;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__nickname {
    font-size: 22rpx;
    color: #a0b4c8;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 140rpx;
  }

  &__phone {
    display: block;
    font-size: 24rpx;
    color: #5b708a;
    margin-bottom: 8rpx;
  }

  &__meta {
    display: flex;
    gap: 12rpx;
  }

  &__meta-item {
    font-size: 22rpx;
    color: #4a86c4;
    background: #eaf2fc;
    border-radius: 10rpx;
    padding: 4rpx 14rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 320rpx;
  }

  &__time {
    flex-shrink: 0;
    margin-left: 12rpx;
  }

  &__time-text {
    font-size: 20rpx;
    color: #b0c2d4;
  }

  &__loading {
    padding: 120rpx 0;
    text-align: center;
  }

  &__loading-text {
    font-size: 28rpx;
    color: #8aa0b8;
  }

  &__empty {
    padding: 120rpx 0;
    text-align: center;
  }

  &__empty-text {
    font-size: 28rpx;
    color: #8aa0b8;
  }

  &__more {
    padding: 20rpx 0;
    text-align: center;
  }

  &__more-text {
    font-size: 26rpx;
    color: #4a86c4;
  }
}
</style>
