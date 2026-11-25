<template>
  <view class="promotion">
    <NavBar title="宣传资料管理" />

    <!-- 加载状态 -->
    <view v-if="loading" class="promotion__loading">
      <text class="promotion__loading-text">加载中...</text>
    </view>

    <!-- 加载失败提示：表单为空是因为加载失败，避免老师误以为没资料并保存覆盖 -->
    <view v-else-if="loadFailed" class="promotion__load-error">
      <text class="promotion__load-error-text">⚠️ 宣传资料加载失败</text>
      <button class="promotion__load-error-btn" @tap="loadExistingData">重新加载</button>
    </view>

    <view v-else class="promotion__body">
      <!-- 大学基本信息 -->
      <view class="promotion__section">
        <text class="promotion__section-title">🏫 基本信息</text>
        <view class="promotion__card">
          <view class="form-group">
            <text class="form-label">大学名称</text>
            <view class="form-value">{{ universityName }}</view>
          </view>
        </view>
      </view>

      <!-- 校徽/Logo -->
      <view class="promotion__section">
        <text class="promotion__section-title">🎨 校徽与头图</text>
        <view class="promotion__card">
          <!-- 校徽上传 -->
          <view class="form-group">
            <text class="form-label">校徽 Logo</text>
            <text class="form-hint">建议尺寸 200x200，PNG格式</text>
            <view class="upload-area" @tap="chooseLogo">
              <image v-if="form.logoUrl" class="upload-preview" :src="resolveAssetUrl(form.logoUrl)" mode="aspectFill" />
              <view v-else class="upload-placeholder">
                <text class="upload-placeholder__icon">🏫</text>
                <text class="upload-placeholder__text">点击上传校徽</text>
              </view>
            </view>
          </view>

          <!-- 头图上传 -->
          <view class="form-group">
            <text class="form-label">宣传头图</text>
            <text class="form-hint">建议尺寸 750x400，JPG格式，用于首页展示</text>
            <view class="upload-area upload-area--wide" @tap="chooseBanner">
              <image v-if="form.bannerUrl" class="upload-preview" :src="resolveAssetUrl(form.bannerUrl)" mode="aspectFill" />
              <view v-else class="upload-placeholder">
                <text class="upload-placeholder__icon">🖼️</text>
                <text class="upload-placeholder__text">点击上传头图</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 校园风光（多图） -->
      <view class="promotion__section">
        <text class="promotion__section-title">📷 校园风光</text>
        <text class="form-hint" style="margin-bottom:16rpx">上传校园照片，最多9张</text>
        <view class="promotion__card">
          <view class="gallery-grid">
            <view
              v-for="(img, index) in form.galleryImages"
              :key="index"
              class="gallery-item"
            >
              <image class="gallery-img" :src="resolveAssetUrl(img)" mode="aspectFill" />
              <view class="gallery-delete" @tap="removeGallery(index)">
                <text class="gallery-delete__icon">✕</text>
              </view>
            </view>
            <view
              v-if="form.galleryImages.length < 9"
              class="gallery-add"
              @tap="chooseGallery"
            >
              <text class="gallery-add__icon">+</text>
              <text class="gallery-add__text">添加照片</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 学校简介 -->
      <view class="promotion__section">
        <text class="promotion__section-title">📝 学校简介</text>
        <view class="promotion__card">
          <view class="form-group">
            <text class="form-label">校训/标语</text>
            <input class="form-input" v-model="form.slogan" placeholder="如：自强不息，厚德载物" maxlength="50" />
          </view>
          <view class="form-group">
            <text class="form-label">学校标签</text>
            <text class="form-hint">用逗号分隔多个标签，如：985,211,双一流</text>
            <input class="form-input" v-model="form.tagsStr" placeholder="如：985,211,双一流,C9" maxlength="100" />
          </view>
          <view class="form-group">
            <text class="form-label">学校简介</text>
            <text class="form-hint">详细介绍学校的优势、特色和历史</text>
            <textarea
              class="form-textarea"
              v-model="form.description"
              placeholder="请输入学校简介..."
              maxlength="5000"
              :auto-height="true"
            />
            <text class="form-counter">{{ form.description.length }}/5000</text>
          </view>
        </view>
      </view>

      <!-- 专业介绍 -->
      <view class="promotion__section">
        <text class="promotion__section-title">📚 专业介绍</text>
        <view class="promotion__card">
          <view class="form-group">
            <text class="form-label">优势专业</text>
            <text class="form-hint">每行一个专业，格式：专业名称|简介</text>
            <textarea
              class="form-textarea"
              v-model="form.majorsStr"
              placeholder="格式：专业名称|简介，每行一个专业"
              :auto-height="true"
            />
          </view>
        </view>
      </view>

      <!-- 分数线 -->
      <view class="promotion__section">
        <text class="promotion__section-title">📊 历年分数线</text>
        <view class="promotion__card">
          <text class="form-hint" style="margin-bottom:20rpx">填写近几年的录取分数线信息</text>
          <view v-for="(score, index) in form.scoreLines" :key="index" class="score-item">
            <view class="score-item__header">
              <text class="score-item__year">第{{ index + 1 }}组</text>
              <view class="score-item__delete" @tap="removeScore(index)">
                <text class="score-item__delete-text">删除</text>
              </view>
            </view>
            <view class="score-item__row">
              <view class="mini-field">
                <text class="mini-label">年份</text>
                <input class="mini-input" v-model="score.year" type="number" placeholder="2024" />
              </view>
              <view class="mini-field">
                <text class="mini-label">理科最低分</text>
                <input class="mini-input" v-model="score.scienceMin" type="number" placeholder="650" />
              </view>
            </view>
            <view class="score-item__row">
              <view class="mini-field">
                <text class="mini-label">文科最低分</text>
                <input class="mini-input" v-model="score.artMin" type="number" placeholder="630" />
              </view>
              <view class="mini-field">
                <text class="mini-label">最低位次</text>
                <input class="mini-input" v-model="score.minRank" type="number" placeholder="5000" />
              </view>
            </view>
          </view>
          <view class="add-score-btn" @tap="addScore">
            <text class="add-score-btn__text">+ 添加分数线</text>
          </view>
        </view>
      </view>

      <!-- 招生政策 -->
      <view class="promotion__section">
        <text class="promotion__section-title">📋 招生政策</text>
        <view class="promotion__card">
          <view class="form-group">
            <text class="form-label">招生政策说明</text>
            <text class="form-hint">介绍招生计划、特殊政策、奖学金等信息</text>
            <textarea
              class="form-textarea"
              v-model="form.admissionPolicy"
              placeholder="请输入招生政策..."
              maxlength="3000"
              :auto-height="true"
            />
            <text class="form-counter">{{ form.admissionPolicy.length }}/3000</text>
          </view>
          <view class="form-group">
            <text class="form-label">招生官网地址</text>
            <input class="form-input" v-model="form.admissionUrl" placeholder="如：https://zsb.tsinghua.edu.cn" />
          </view>
        </view>
      </view>

      <!-- 保存按钮 -->
      <view class="promotion__save-bar">
        <button class="save-btn save-btn--preview" @tap="handlePreview">
          <text class="save-btn__text">预览效果</text>
        </button>
        <button class="save-btn save-btn--submit" @tap="handleSave" :loading="saving">
          <text class="save-btn__text">保存资料</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import { useUserStore } from '@/store/modules/user'
import * as promotionApi from '@/api/promotion'
import { BASE_URL } from '@/api/index'
import { getToken } from '@/utils/auth'

const userStore = useUserStore()

const loading = ref(true)
const saving = ref(false)
// 加载失败标志：失败时禁止保存，防止空表单覆盖服务器已有宣传资料
const loadFailed = ref(false)

const universityName = computed(() => userStore.userInfo?.universityName || '本校')
const universityId = computed(() => userStore.userInfo?.universityId)

const form = reactive({
  logoUrl: '',
  bannerUrl: '',
  galleryImages: [],
  slogan: '',
  tagsStr: '',
  description: '',
  majorsStr: '',
  scoreLines: [],
  admissionPolicy: '',
  admissionUrl: ''
})

onLoad(() => {
  verifyPermissionAndLoad()
})

async function verifyPermissionAndLoad() {
  try {
    const status = await promotionApi.getManagerStatus()
    if (!status?.manager) {
      loading.value = false
      uni.showToast({ title: '未获得宣传资料管理权限', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 800)
      return
    }
    await loadExistingData()
  } catch (e) {
    loading.value = false
    uni.showToast({ title: e.message || '权限校验失败', icon: 'none' })
  }
}

// 从后端加载已有的宣传资料
async function loadExistingData() {
  loading.value = true
  loadFailed.value = false
  const uid = universityId.value
  if (!uid) {
    loading.value = false
    return
  }
  try {
    const data = await promotionApi.getPromotion(uid)
    if (data) {
      // 将后端字段映射到表单
      form.logoUrl = data.logoUrl || ''
      form.bannerUrl = data.bannerUrl || ''
      form.slogan = data.slogan || ''
      form.tagsStr = data.tags || ''
      form.description = data.description || ''
      form.admissionPolicy = data.admissionPolicy || ''
      form.admissionUrl = data.admissionUrl || ''

      // 解析 JSON 字段
      try {
        form.galleryImages = data.galleryImages ? JSON.parse(data.galleryImages) : []
      } catch (e) { form.galleryImages = [] }
      try {
        const majors = data.majors ? JSON.parse(data.majors) : []
        form.majorsStr = majors.map(m => m.name + '|' + (m.description || '')).join('\n')
      } catch (e) {
        // 兼容逗号分隔的历史数据（如"临床医学,基础医学"）
        form.majorsStr = String(data.majors || '').split(',')
          .map(s => s.trim()).filter(Boolean).join('\n')
      }
      try {
        form.scoreLines = data.scoreLines ? JSON.parse(data.scoreLines) : []
      } catch (e) { form.scoreLines = [] }
    }
  } catch (e) {
    console.error('加载宣传资料失败:', e)
    // 尝试从本地存储恢复
    let restored = false
    try {
      const cached = uni.getStorageSync('promotion_draft_' + universityId.value)
      if (cached) {
        const data = JSON.parse(cached)
        Object.assign(form, data)
        restored = true
      }
    } catch (e2) { /* ignore */ }
    // 本地也无可恢复数据：标记失败并禁止保存，防止空表单覆盖服务器已有内容
    loadFailed.value = !restored
  } finally {
    loading.value = false
  }
}

// 图片展示:后端存的是相对路径(/uploads/xxx.jpg),展示时拼 BASE_URL;wxfile 临时路径原样返回
function resolveAssetUrl(url) {
  if (!url) return ''
  if (/^(https?|wxfile):\/\//.test(url)) return url
  if (url.startsWith('/')) return BASE_URL + url
  return url
}

// 上传图片到服务器,成功返回相对路径(/uploads/xxx)
function uploadImage(filePath) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/api/upload/image',
      filePath: filePath,
      name: 'file',
      // 上传接口需登录，uploadFile 不会自动带 token，必须手动携带
      header: { Authorization: `Bearer ${getToken() || ''}` },
      success: (uploadRes) => {
        if (uploadRes.statusCode !== 200) {
          reject(new Error('上传失败'))
          return
        }
        try {
          const data = JSON.parse(uploadRes.data)
          if (data.code === 200 && data.data) {
            resolve(data.data)
          } else {
            reject(new Error(data.message || '上传失败'))
          }
        } catch (e) {
          reject(e)
        }
      },
      fail: reject
    })
  })
}

// 选择校徽
function chooseLogo() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      uni.showLoading({ title: '上传中...' })
      uploadImage(res.tempFilePaths[0])
        .then((url) => {
          form.logoUrl = url
        })
        .catch(() => {
          uni.showToast({ title: '校徽上传失败，请重试', icon: 'none' })
        })
        .finally(() => {
          uni.hideLoading()
        })
    }
  })
}

// 选择头图
function chooseBanner() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      uni.showLoading({ title: '上传中...' })
      uploadImage(res.tempFilePaths[0])
        .then((url) => {
          form.bannerUrl = url
        })
        .catch(() => {
          uni.showToast({ title: '头图上传失败，请重试', icon: 'none' })
        })
        .finally(() => {
          uni.hideLoading()
        })
    }
  })
}

// 添加校园风光照片
function chooseGallery() {
  const remaining = 9 - form.galleryImages.length
  uni.chooseImage({
    count: remaining,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      uni.showLoading({ title: '上传中...' })
      const tasks = res.tempFilePaths.map((filePath) =>
        uploadImage(filePath)
          .then((url) => {
            form.galleryImages.push(url)
            return true
          })
          .catch(() => {
            uni.showToast({ title: '部分照片上传失败', icon: 'none' })
            return false
          })
      )
      Promise.all(tasks).finally(() => {
        uni.hideLoading()
      })
    }
  })
}

// 删除校园风光照片
function removeGallery(index) {
  form.galleryImages.splice(index, 1)
}

// 添加分数线
function addScore() {
  form.scoreLines.push({
    year: '',
    scienceMin: '',
    artMin: '',
    minRank: ''
  })
}

// 删除分数线
function removeScore(index) {
  form.scoreLines.splice(index, 1)
}

// 保存
async function handleSave() {
  if (saving.value) return
  // 加载失败保护：防止空表单覆盖服务器已有宣传资料
  if (loadFailed.value) {
    uni.showModal({
      title: '无法保存',
      content: '宣传资料加载失败，为避免覆盖已有内容，请先重新加载后再保存',
      confirmText: '重新加载',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) loadExistingData()
      }
    })
    return
  }
  saving.value = true

  try {
    uni.showLoading({ title: '保存中...' })

    // 构建提交数据
    const submitData = buildSubmitData()

    // 调用后端API保存
    await promotionApi.savePromotion(submitData)

    // 同时保存到本地存储作为备份
    uni.setStorageSync('promotion_draft_' + universityId.value, JSON.stringify(form))

    uni.hideLoading()
    uni.showToast({ title: '保存成功', icon: 'success' })
  } catch (e) {
    uni.hideLoading()
    console.error('保存失败:', e)
    // 保存失败时先保存到本地
    uni.setStorageSync('promotion_draft_' + universityId.value, JSON.stringify(form))
    uni.showToast({ title: '已保存到本地: ' + (e.message || '请重试'), icon: 'none' })
  } finally {
    saving.value = false
  }
}

function buildSubmitData() {
  return {
    universityId: universityId.value,
    logoUrl: form.logoUrl,
    bannerUrl: form.bannerUrl,
    galleryImages: form.galleryImages,
    slogan: form.slogan,
    tags: form.tagsStr.split(',').map(t => t.trim()).filter(Boolean),
    description: form.description,
    majors: form.majorsStr.split('\n').filter(Boolean).map(line => {
      const [name, description] = line.split('|')
      return { name: name?.trim() || '', description: description?.trim() || '' }
    }),
    scoreLines: form.scoreLines.filter(s => s.year),
    admissionPolicy: form.admissionPolicy,
    admissionUrl: form.admissionUrl
  }
}

// 预览效果
function handlePreview() {
  // 跳转到大学详情页预览
  if (universityId.value) {
    uni.navigateTo({ url: `/pages/common/university-detail?id=${universityId.value}&preview=1` })
  } else {
    uni.showToast({ title: '请先保存', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.promotion {
  min-height: 100vh;
  background: #f0f2f5;

  &__loading {
    display: flex;
    justify-content: center;
    padding: 120rpx 0;

    &-text {
      font-size: 28rpx;
      color: #a0aec0;
    }
  }

  &__load-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 120rpx 48rpx;
    gap: 24rpx;

    &-text {
      font-size: 28rpx;
      color: #e6a23c;
      text-align: center;
    }

    &-btn {
      font-size: 26rpx;
      color: #fff;
      background: #5B9BD5;
      border-radius: 999rpx;
      padding: 0 48rpx;
      height: 72rpx;
      line-height: 72rpx;

      &::after {
        border: none;
      }
    }
  }

  &__body {
    padding-bottom: 160rpx;
  }

  &__section {
    padding: 0 24rpx;
    margin-bottom: 24rpx;

    &-title {
      font-size: 30rpx;
      font-weight: 700;
      color: #2d3748;
      display: block;
      margin-bottom: 16rpx;
    }
  }

  &__card {
    background: #fff;
    border-radius: 16rpx;
    padding: 28rpx 24rpx;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
  }

  &__save-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: #fff;
    padding: 20rpx 24rpx;
    padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
    display: flex;
    gap: 16rpx;
    box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
    z-index: 100;
  }
}

// 表单元素
.form-group {
  margin-bottom: 24rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #2d3748;
  display: block;
  margin-bottom: 8rpx;
}

.form-hint {
  font-size: 22rpx;
  color: #a0aec0;
  display: block;
  margin-bottom: 12rpx;
}

.form-value {
  font-size: 28rpx;
  color: #4a5568;
  background: #f7fafc;
  border-radius: 12rpx;
  padding: 24rpx;
}

.form-input {
  width: 100%;
  height: 88rpx;
  background: #f7fafc;
  border: 2rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #2d3748;

  &:focus {
    border-color: #4A8BC4;
    background: #fff;
  }
}

.form-textarea {
  width: 100%;
  min-height: 180rpx;
  background: #f7fafc;
  border: 2rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 26rpx;
  color: #2d3748;
  line-height: 1.6;

  &:focus {
    border-color: #4A8BC4;
    background: #fff;
  }
}

.form-counter {
  font-size: 22rpx;
  color: #a0aec0;
  text-align: right;
  display: block;
  margin-top: 8rpx;
}

// 上传区域
.upload-area {
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
  overflow: hidden;
  border: 2rpx dashed #cbd5e0;
  background: #f7fafc;

  &--wide {
    width: 100%;
    height: 260rpx;
  }
}

.upload-preview {
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  &__icon {
    font-size: 48rpx;
    margin-bottom: 8rpx;
  }

  &__text {
    font-size: 22rpx;
    color: #a0aec0;
  }
}

// 图片网格
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}

.gallery-item {
  position: relative;
  border-radius: 12rpx;
  overflow: hidden;
  aspect-ratio: 1;
}

.gallery-img {
  width: 100%;
  height: 100%;
}

.gallery-delete {
  position: absolute;
  top: 6rpx;
  right: 6rpx;
  width: 40rpx;
  height: 40rpx;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;

  &__icon {
    font-size: 22rpx;
    color: #fff;
  }
}

.gallery-add {
  aspect-ratio: 1;
  border: 2rpx dashed #cbd5e0;
  border-radius: 12rpx;
  background: #f7fafc;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  &__icon {
    font-size: 48rpx;
    color: #a0aec0;
  }

  &__text {
    font-size: 20rpx;
    color: #a0aec0;
    margin-top: 4rpx;
  }
}

// 分数线
.score-item {
  background: #f7fafc;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16rpx;
  }

  &__year {
    font-size: 26rpx;
    font-weight: 600;
    color: #4a5568;
  }

  &__delete {
    &-text {
      font-size: 22rpx;
      color: #e53e3e;
    }
  }

  &__row {
    display: flex;
    gap: 16rpx;
    margin-bottom: 12rpx;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.mini-field {
  flex: 1;

  .mini-label {
    font-size: 22rpx;
    color: #718096;
    display: block;
    margin-bottom: 6rpx;
  }

  .mini-input {
    width: 100%;
    height: 64rpx;
    background: #fff;
    border: 2rpx solid #e2e8f0;
    border-radius: 8rpx;
    padding: 0 16rpx;
    font-size: 24rpx;
    color: #2d3748;
  }
}

.add-score-btn {
  border: 2rpx dashed #cbd5e0;
  border-radius: 12rpx;
  padding: 20rpx;
  text-align: center;

  &__text {
    font-size: 26rpx;
    color: #4A8BC4;
  }
}

// 保存按钮
.save-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;

  &::after {
    display: none;
  }

  &--preview {
    background: #edf2f7;

    .save-btn__text {
      color: #4a5568;
    }
  }

  &--submit {
    background: linear-gradient(135deg, #3A7BAA, #4A8BC4);
    box-shadow: 0 6rpx 18rpx rgba(26, 54, 93, 0.25);

    .save-btn__text {
      color: #fff;
    }
  }

  &__text {
    font-size: 28rpx;
    font-weight: 600;
  }
}
</style>
