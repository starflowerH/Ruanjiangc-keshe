<template>
  <view class="register">
    <NavBar title="志愿者报名" />

    <view class="register__loading" v-if="!pageReady">
      <text class="register__loading-text">正在检查报名状态...</text>
    </view>

    <view class="register__content" v-else>
      <!-- 选择大学 -->
      <view class="register__field">
        <text class="register__label">所属大学 <text class="register__required">*</text></text>
        <UniversitySearch
          :selected-id="form.universityId"
          :selected-name="form.universityName"
          placeholder="搜索并选择所属大学"
          @select="onUniversitySelect"
        />
      </view>

      <!-- 基本信息 -->
      <view class="register__field">
        <text class="register__label">姓名 <text class="register__required">*</text></text>
        <input class="register__input" v-model="form.name" placeholder="请输入真实姓名" />
      </view>

      <view class="register__field">
        <text class="register__label">学号 <text class="register__required">*</text></text>
        <input class="register__input" v-model="form.studentNo" placeholder="请输入学号" />
      </view>

      <view class="register__field">
        <text class="register__label">性别 <text class="register__required">*</text></text>
        <picker :range="genderOptions" @change="onGenderChange">
          <view class="register__picker">
            <text :class="['register__picker-text', !form.gender && 'register__picker-text--placeholder']">
              {{ form.gender || '请选择性别' }}
            </text>
            <text class="register__picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="register__field">
        <text class="register__label">电话或邮箱至少填写一项 <text class="register__required">*</text></text>
      <input class="register__input" v-model="form.phone" type="number" maxlength="11" placeholder="请输入手机号（选填）" />
        <input class="register__input register__input--spaced" v-model="form.email" type="text" placeholder="请输入邮箱（选填）" />
      </view>

      <view class="register__field">
        <text class="register__label">年级 <text class="register__required">*</text></text>
        <picker :range="gradeOptions" @change="onGradeChange">
          <view class="register__picker">
            <text :class="['register__picker-text', !form.grade && 'register__picker-text--placeholder']">
              {{ form.grade || '请选择年级' }}
            </text>
            <text class="register__picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="register__field">
        <text class="register__label">专业 <text class="register__required">*</text></text>
        <input class="register__input" v-model="form.major" placeholder="请输入专业" />
      </view>

      <!-- 目标高中 -->
      <view class="register__field">
        <text class="register__label">目标高中（选填）</text>
        <text class="register__hint">填写后可用于匹配队伍和队长通讯录</text>
        <HighSchoolSearch
          :selected-id="form.targetHighSchoolId"
          :selected-name="form.targetHighSchoolName"
          placeholder="搜索并选择目标高中"
          @select="onHighSchoolSelect"
        />
      </view>

      <!-- 申请队长 -->
      <view class="register__field register__field--row">
        <view class="register__field-left">
          <text class="register__label">申请队长</text>
          <text class="register__hint">队长可以查看其他队长联系方式，便于协调沟通</text>
        </view>
        <switch :checked="form.isCaptain" @change="onCaptainChange" color="#5B9BD5" />
      </view>

      <!-- 提交 -->
      <view class="register__btn" @tap="handleSubmit">
        <text class="register__btn-text">提交报名</text>
      </view>

      <text class="register__tip">提交后请等待管理员审核确认</text>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import HighSchoolSearch from '@/components/HighSchoolSearch.vue'
import UniversitySearch from '@/components/UniversitySearch.vue'
import { useUserStore } from '@/store/modules/user'
import * as enrollmentApi from '@/api/enrollment'
import * as authApi from '@/api/auth'

const uniApi = uni
const userStore = useUserStore()
const universityList = ref([])
const universityNames = ref([])
const pageReady = ref(false)
const redirectedToHome = ref(false)
const gradeOptions = ['大一', '大二', '大三', '大四', '研一', '研二', '研三']
const genderOptions = ['男', '女', '其他']

const form = reactive({
  universityId: null,
  universityName: '',
  name: '',
  studentNo: '',
  gender: '',
  phone: '',
  email: '',
  grade: '',
  major: '',
  targetHighSchoolId: null,
  targetHighSchoolName: '',
  isCaptain: false
})

onLoad(async () => {
  form.name = userStore.userInfo?.nickname || userStore.userInfo?.name || ''
  await guardExistingApply()
  if (!redirectedToHome.value) {
    await loadOptions()
    pageReady.value = true
  }
})

async function guardExistingApply() {
  try {
    const data = await enrollmentApi.getMyEnrollment()
    const list = Array.isArray(data) ? data : (data?.records || [])
    const latestApply = list[0]
    if (!latestApply || !['APPROVED', 'PENDING'].includes(latestApply.status)) {
      return
    }

    redirectedToHome.value = true
    userStore.updateUserInfo({
      relatedId: latestApply.id,
      userType: 'volunteer',
      status: latestApply.status,
      name: latestApply.name,
      nickname: latestApply.name,
      phone: latestApply.phone,
      wechatId: latestApply.wechatId,
      grade: latestApply.grade,
      major: latestApply.major,
      universityName: latestApply.universityName,
      targetHighSchoolName: latestApply.highSchoolName
    })

    uniApi.showModal({
      title: latestApply.status === 'APPROVED' ? '报名已通过' : '报名审核中',
      content: latestApply.status === 'APPROVED'
        ? '你的志愿者报名已经通过，无需重复填写。'
        : '你的志愿者报名正在审核中，无需重复提交。',
      showCancel: false,
      confirmText: '返回首页',
      success: () => uniApi.reLaunch({ url: '/pages/volunteer/home' })
    })
  } catch (e) {
    // A failed status check should not trap a user on a loading screen.
    console.error('检查报名状态失败:', e)
  }
}

async function loadOptions() {
  try {
    const uniRes = await enrollmentApi.getUniversityList()

    if (uniRes) {
      universityList.value = uniRes
      universityNames.value = uniRes.map(u => u.name)
    }
  } catch (e) {
    console.error('加载选项失败:', e)
    universityList.value = []
    universityNames.value = []
  }
}

function onUniversitySelect(university) {
  form.universityId = university.id
  form.universityName = university.name
}

function onHighSchoolSelect(school) {
  form.targetHighSchoolId = school.id
  form.targetHighSchoolName = school.name
}

function onGradeChange(e) {
  form.grade = gradeOptions[e.detail.value]
}

function onGenderChange(e) {
  form.gender = genderOptions[e.detail.value]
}

function onCaptainChange(e) {
  form.isCaptain = e.detail.value
}

async function handleSubmit() {
  // 表单验证
  if (!form.universityId) {
    uniApi.showToast({ title: '请选择所属大学', icon: 'none' })
    return
  }
  if (!form.name.trim()) {
    uniApi.showToast({ title: '请输入姓名', icon: 'none' })
    return
  }
  if (!form.studentNo.trim()) {
    uniApi.showToast({ title: '请输入学号', icon: 'none' })
    return
  }
  if (!form.gender) {
    uniApi.showToast({ title: '请选择性别', icon: 'none' })
    return
  }
  const phone = form.phone.trim()
  const email = form.email.trim()
  if (!phone && !email) {
    uniApi.showToast({ title: '电话或邮箱至少填写一项', icon: 'none' })
    return
  }
  if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
    uniApi.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    uniApi.showToast({ title: '请输入正确的邮箱地址', icon: 'none' })
    return
  }
  if (!form.grade) {
    uniApi.showToast({ title: '请选择年级', icon: 'none' })
    return
  }
  if (!form.major) {
    uniApi.showToast({ title: '请输入专业', icon: 'none' })
    return
  }
  uniApi.showLoading({ title: '提交中...' })

  try {
    await enrollmentApi.submitEnrollment({
      universityId: form.universityId,
      name: form.name.trim(),
      studentNo: form.studentNo.trim(),
      gender: form.gender,
      phone: phone,
      email: email,
      grade: form.grade,
      major: form.major,
      targetHighSchoolId: form.targetHighSchoolId,
      isCaptainApplicant: form.isCaptain ? 1 : 0
    })

    try {
      const identity = await authApi.switchIdentity('volunteer')
      userStore.setWechatIdentity(identity, 'volunteer')
    } catch (switchError) {
      console.error('报名成功但身份刷新失败:', switchError)
      userStore.logout()
      uniApi.hideLoading()
      uniApi.showToast({ title: '报名已提交，请重新登录', icon: 'none' })
      setTimeout(() => {
        uniApi.reLaunch({ url: '/pages/common/login?role=volunteer' })
      }, 1500)
      return
    }

    uniApi.hideLoading()
    uniApi.showToast({ title: '报名成功', icon: 'success' })

    setTimeout(() => {
      uniApi.reLaunch({ url: '/pages/volunteer/home' })
    }, 1500)
  } catch (e) {
    uniApi.hideLoading()
    console.error('报名失败:', e)
    // 异常机制：失败弹窗展示具体原因，不吞错误
    uniApi.showModal({
      title: '报名失败',
      content: e.message || '网络异常，请稍后重试',
      showCancel: false,
      confirmText: '知道了'
    })
  }
}
</script>

<style lang="scss" scoped>
.register {
  min-height: 100vh;
  background: #F2F6FB;

  &__loading {
    min-height: 50vh;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__loading-text {
    color: #8A96A3;
    font-size: 28rpx;
  }

  &__content {
    padding: 24rpx;
  }

  &__field {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 16rpx;

    &--row {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
  }

  &__field-left {
    flex: 1;
    margin-right: 16rpx;
  }

  &__label {
    font-size: 28rpx;
    font-weight: 500;
    color: #333;
    display: block;
    margin-bottom: 12rpx;
  }

  &__required {
    color: #ff4d4f;
  }

  &__hint {
    font-size: 22rpx;
    color: #999;
    display: block;
    margin-bottom: 12rpx;
  }

  &__input {
    width: 100%;
    height: 80rpx;
    border: 2rpx solid #e8e8e8;
    border-radius: 12rpx;
    padding: 0 24rpx;
    font-size: 28rpx;
    background: #fafafa;
  }

  &__input--spaced {
    margin-top: 16rpx;
  }

  &__picker {
    height: 80rpx;
    border: 2rpx solid #e8e8e8;
    border-radius: 12rpx;
    padding: 0 24rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #fafafa;
  }

  &__picker-text {
    font-size: 28rpx;
    color: #333;

    &--placeholder {
      color: #999;
    }
  }

  &__picker-arrow {
    font-size: 32rpx;
    color: #ccc;
  }

  &__btn {
    width: 100%;
    height: 96rpx;
    background: linear-gradient(135deg, #5B9BD5, #4A8BC4);
    border-radius: 12rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 32rpx;
  }

  &__btn-text {
    font-size: 32rpx;
    color: #fff;
    font-weight: 600;
  }

  &__tip {
    font-size: 24rpx;
    color: #999;
    text-align: center;
    display: block;
    margin-top: 24rpx;
  }
}
</style>
