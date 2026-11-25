<template>
  <view class="uni-search">
    <view class="uni-search__trigger" @tap="open">
      <text :class="selectedName ? 'uni-search__value' : 'uni-search__placeholder'">
        {{ selectedName || placeholder }}
      </text>
      <text class="uni-search__arrow">›</text>
    </view>

    <view v-if="visible" class="uni-search__mask" :style="maskStyle" @tap="close">
      <view class="uni-search__panel" :style="panelStyle" @tap.stop>
        <view class="uni-search__header">
          <text class="uni-search__title">选择大学</text>
          <view class="uni-search__close" @tap="close" aria-label="关闭">
            <text class="uni-search__close-icon">×</text>
          </view>
        </view>

        <view class="uni-search__input-wrap">
          <input
            class="uni-search__input"
            v-model="keyword"
            :focus="inputFocused"
            confirm-type="search"
            placeholder="输入大学名称搜索"
            @input="handleInput"
            @focus="onInputFocused"
            @confirm="applyFilter"
          />
          <view v-if="keyword" class="uni-search__clear" @tap="clearKeyword" aria-label="清空">
            <text class="uni-search__clear-icon">×</text>
          </view>
        </view>

        <scroll-view class="uni-search__results" scroll-y>
          <view v-if="loading" class="uni-search__state">
            <text>加载中...</text>
          </view>
          <view v-else-if="errorMessage" class="uni-search__state">
            <text>{{ errorMessage }}</text>
            <text class="uni-search__retry" @tap="loadUniversities">重试</text>
          </view>
          <view v-else-if="!results.length" class="uni-search__state">
            <text>未找到匹配的大学</text>
          </view>
          <view
            v-for="university in results"
            :key="university.id"
            class="uni-search__item"
            :class="{ 'uni-search__item--selected': university.id === selectedId }"
            @tap="selectUniversity(university)"
          >
            <view class="uni-search__item-content">
              <text class="uni-search__item-name">{{ university.name }}</text>
              <text v-if="university.description" class="uni-search__item-location">
                {{ university.description }}
              </text>
            </view>
            <text v-if="university.id === selectedId" class="uni-search__check">✓</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { getUniversityList } from '@/api/university'

const props = defineProps({
  selectedId: {
    type: [Number, String],
    default: null
  },
  selectedName: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请选择大学'
  },
  zIndex: {
    type: [Number, String],
    default: 1200
  }
})

const emit = defineEmits(['select'])
const visible = ref(false)
const keyword = ref('')
const results = ref([])
const allUniversities = ref([])
const loading = ref(false)
const errorMessage = ref('')
const keyboardHeight = ref(0)
const inputFocused = ref(false)
let offKeyboard = null
let lastKbUpdate = 0
let loaded = false

// 键盘弹起时给面板底部让出键盘高度，避免遮挡搜索结果
const panelStyle = computed(() => ({
  paddingBottom: (keyboardHeight.value > 0 ? keyboardHeight.value : 0) + 'px'
}))

// 搜索遮罩层级可配置：嵌套在自定义弹层(sheet/mask)内时需高于外层弹层层级
const maskStyle = computed(() => ({
  zIndex: props.zIndex
}))

// 只触发一次自动聚焦，避免重渲染导致键盘反复弹起（真机输入失效的根因）
function onInputFocused() {
  inputFocused.value = false
}

function open() {
  visible.value = true
  keyword.value = ''
  inputFocused.value = true
  loadUniversities()
  // 微信小程序：监听键盘高度变化（节流，防止高频重渲染）
  // #ifdef MP-WEIXIN
  if (!offKeyboard) {
    offKeyboard = uni.onKeyboardHeightChange((res) => {
      const h = res?.height || 0
      const now = Date.now()
      if (h === keyboardHeight.value) return
      if (now - lastKbUpdate < 200) return
      lastKbUpdate = now
      keyboardHeight.value = h
    })
  }
  // #endif
}

function close() {
  visible.value = false
  keyword.value = ''
}

async function loadUniversities() {
  if (loaded && allUniversities.value.length) {
    applyFilter()
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getUniversityList()
    allUniversities.value = Array.isArray(data) ? data : (data?.records || [])
    loaded = true
    applyFilter()
  } catch (e) {
    errorMessage.value = e.message || '加载失败'
    results.value = []
  } finally {
    loading.value = false
  }
}

function handleInput() {
  applyFilter()
}

function clearKeyword() {
  keyword.value = ''
  applyFilter()
}

// 本地过滤：按名称/简介匹配关键词
function applyFilter() {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) {
    results.value = allUniversities.value
    return
  }
  results.value = allUniversities.value.filter((u) =>
    (u.name || '').toLowerCase().includes(kw) ||
    (u.description || '').toLowerCase().includes(kw)
  )
}

function selectUniversity(university) {
  emit('select', university)
  close()
}

onUnmounted(() => {
  // #ifdef MP-WEIXIN
  if (offKeyboard) {
    offKeyboard()
    offKeyboard = null
  }
  // #endif
  keyboardHeight.value = 0
})
</script>

<style lang="scss" scoped>
.uni-search {
  width: 100%;

  &__trigger {
    min-height: 80rpx;
    border: 2rpx solid #e2e8f0;
    border-radius: 8rpx;
    padding: 18rpx 24rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #fafafa;
  }

  &__value,
  &__placeholder {
    flex: 1;
    min-width: 0;
    font-size: 28rpx;
    line-height: 40rpx;
  }

  &__value { color: #222; }
  &__placeholder { color: #999; }
  &__arrow { margin-left: 16rpx; font-size: 34rpx; color: #94a3b8; }

  &__mask {
    position: fixed;
    inset: 0;
    z-index: 1200;
    display: flex;
    align-items: flex-end;
    background: rgba(0, 0, 0, 0.45);
  }

  &__panel {
    width: 100%;
    height: 72vh;
    max-height: 1000rpx;
    padding-bottom: env(safe-area-inset-bottom);
    background: #fff;
    border-radius: 8px 8px 0 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  &__header {
    height: 104rpx;
    padding: 0 24rpx 0 32rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 2rpx solid #f1f5f9;
  }

  &__title { font-size: 32rpx; font-weight: 600; color: #1f2937; }
  &__close,
  &__clear {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  &__close-icon { font-size: 44rpx; color: #64748b; }
  &__clear-icon { font-size: 34rpx; color: #94a3b8; }

  &__input-wrap {
    height: 80rpx;
    margin: 20rpx 24rpx;
    padding-left: 24rpx;
    display: flex;
    align-items: center;
    border: 2rpx solid #cbd5e1;
    border-radius: 8rpx;
    background: #f8fafc;
  }

  &__input { flex: 1; height: 76rpx; font-size: 28rpx; color: #1f2937; }
  &__results { flex: 1; min-height: 0; }

  &__item {
    min-height: 112rpx;
    margin: 0 24rpx;
    padding: 20rpx 8rpx;
    display: flex;
    align-items: center;
    border-bottom: 2rpx solid #f1f5f9;
  }

  &__item--selected { background: #f0f7ff; }
  &__item-content { flex: 1; min-width: 0; }
  &__item-name { display: block; font-size: 29rpx; color: #1f2937; line-height: 42rpx; }
  &__item-location { display: block; margin-top: 4rpx; font-size: 23rpx; color: #64748b; line-height: 34rpx; }
  &__check { width: 56rpx; text-align: center; font-size: 30rpx; color: #5B9BD5; }

  &__state {
    min-height: 240rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 20rpx;
    font-size: 26rpx;
    color: #64748b;
  }

  &__retry { color: #5B9BD5; }
}
</style>
