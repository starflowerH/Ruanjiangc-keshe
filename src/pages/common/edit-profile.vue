<template>
  <view class="edit-profile">
    <NavBar :title="isFirstRegistration ? '完善学生信息' : '编辑资料'" />

    <view v-if="loading" class="edit-profile__loading">
      <text>加载中...</text>
    </view>

    <view v-else class="edit-profile__body">
      <!-- 头像 -->
      <view class="edit-profile__section">
        <view class="edit-profile__row" @tap="changeAvatar">
          <text class="edit-profile__label">头像</text>
          <view class="edit-profile__row-right">
            <image class="edit-profile__avatar-img" :src="resolveAvatarUrl(form.avatarUrl) || '/static/images/default-avatar.png'" mode="aspectFill" />
            <text class="edit-profile__arrow">›</text>
          </view>
        </view>
      </view>

      <!-- 昵称（所有角色） -->
      <view class="edit-profile__section">
        <view class="edit-profile__row">
          <text class="edit-profile__label">昵称</text>
          <input class="edit-profile__input" v-model="form.nickname" placeholder="请输入昵称" maxlength="20" />
        </view>
      </view>

      <!-- 真实姓名（志愿者/老师/学生） -->
      <view class="edit-profile__section" v-if="currentRole === 'volunteer' || currentRole === 'teacher' || currentRole === 'student'">
        <view class="edit-profile__row">
          <text class="edit-profile__label">姓名</text>
          <input class="edit-profile__input" v-model="form.name" placeholder="请输入真实姓名" maxlength="20" />
        </view>
      </view>

      <!-- 手机号（所有角色） -->
      <view class="edit-profile__section">
        <view class="edit-profile__row">
          <text class="edit-profile__label">手机号</text>
          <input class="edit-profile__input" v-model="form.phone" type="number" maxlength="11" placeholder="请输入手机号" />
        </view>
      </view>

      <!-- 所在高中（仅学生，首次注册必选） -->
      <view class="edit-profile__section" v-if="currentRole === 'student'">
        <view class="edit-profile__row">
          <text class="edit-profile__label">所在高中</text>
          <view class="edit-profile__school-search">
            <HighSchoolSearch
              :selected-id="form.highSchoolId"
              :selected-name="hsSelectedName"
              placeholder="搜索并选择所在高中"
              @select="onHighSchoolSelect"
            />
          </view>
        </view>
      </view>

      <!-- 所在大学（仅志愿者，可修改；在队伍中需先退队） -->
      <view class="edit-profile__section" v-if="currentRole === 'volunteer'">
        <view class="edit-profile__row">
          <text class="edit-profile__label">所在大学</text>
          <view class="edit-profile__school-search">
            <UniversitySearch
              :selected-id="form.universityId"
              :selected-name="uniSelectedName"
              placeholder="搜索并选择就读大学"
              @select="onUniversitySelect"
            />
          </view>
        </view>
      </view>

      <!-- 微信号（仅志愿者） -->
      <view class="edit-profile__section" v-if="currentRole === 'volunteer'">
        <view class="edit-profile__row">
          <text class="edit-profile__label">微信号</text>
          <input class="edit-profile__input" v-model="form.wechatId" placeholder="请输入微信号" />
        </view>
      </view>

      <!-- 专业（仅志愿者） -->
      <view class="edit-profile__section" v-if="currentRole === 'volunteer'">
        <view class="edit-profile__row">
          <text class="edit-profile__label">专业</text>
          <input class="edit-profile__input" v-model="form.major" placeholder="请输入专业" />
        </view>
      </view>

      <!-- 年级（仅志愿者） -->
      <view class="edit-profile__section" v-if="currentRole === 'volunteer'">
        <view class="edit-profile__row">
          <text class="edit-profile__label">年级</text>
          <picker :range="gradeOptions" :value="gradeIndex" @change="onGradeChange">
            <view class="edit-profile__picker">
              <text :class="form.grade ? 'edit-profile__picker-text' : 'edit-profile__picker-placeholder'">
                {{ form.grade || '请选择年级' }}
              </text>
              <text class="edit-profile__arrow">›</text>
            </view>
          </picker>
        </view>
      </view>

      <!-- 角色信息展示 -->
      <view class="edit-profile__section">
        <view class="edit-profile__row">
          <text class="edit-profile__label">当前身份</text>
          <text class="edit-profile__value">{{ roleLabel }}</text>
        </view>
      </view>

      <!-- 提示 -->
      <text class="edit-profile__tip">
        {{ isFirstRegistration ? '完成后即可使用提问和消息功能' : '修改信息后将立即生效' }}
      </text>

      <!-- 保存 -->
      <view class="edit-profile__btn" @tap="handleSave" :class="{ 'edit-profile__btn--loading': saving }">
        <text class="edit-profile__btn-text">
          {{ saving ? '保存中...' : (isFirstRegistration ? '完成注册' : '保存') }}
        </text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import NavBar from '@/components/NavBar.vue'
import HighSchoolSearch from '@/components/HighSchoolSearch.vue'
import UniversitySearch from '@/components/UniversitySearch.vue'
import * as profileApi from '@/api/profile'
import * as studentApi from '@/api/student'
import { BASE_URL } from '@/api/index'
import { getToken } from '@/utils/auth'

const uniApi = uni
const userStore = useUserStore()
const loading = ref(true)
const saving = ref(false)
const registrationMode = ref(false)

const form = reactive({
  nickname: '',
  avatarUrl: '',
  name: '',
  phone: '',
  wechatId: '',
  major: '',
  grade: '',
  highSchoolId: null,
  universityId: null
})

const gradeOptions = ['大一', '大二', '大三', '大四', '研一', '研二', '研三']
const gradeIndex = ref(-1)

const hsSelectedName = ref('')
const uniSelectedName = ref('')

const currentRole = computed(() => {
  // 优先按登录时的 userRole 判断，避免管理员切学生模式后 currentRole 恒为 admin
  if (userStore.userRole === 'student') return 'student'
  if (userStore.userRole === 'volunteer') return 'volunteer'
  if (userStore.userRole === 'teacher') return 'teacher'
  if (userStore.userRole === 'admin') return 'admin'
  if (userStore.isTeacher) return 'teacher'
  if (userStore.isAdmin && !userStore.isTeacher) return 'admin'
  if (userStore.isVolunteer) return 'volunteer'
  return 'student'
})

const roleLabel = computed(() => {
  const map = { student: '高中生', volunteer: '大学生志愿者', teacher: '大学老师', admin: '管理员' }
  return map[currentRole.value] || ''
})

const isFirstRegistration = computed(() => {
  return currentRole.value === 'student'
    && (registrationMode.value || userStore.userInfo?.relatedId == null)
})

onLoad(async (options) => {
  registrationMode.value = options?.register === '1'
  if (!userStore.isLoggedIn) {
    loading.value = false
    const role = ['student', 'volunteer', 'admin', 'teacher'].includes(userStore.userRole)
      ? userStore.userRole
      : ''
    setTimeout(() => {
      uniApi.reLaunch({
        url: role ? `/pages/common/login?role=${role}` : '/pages/common/login'
      })
    }, 100)
    return
  }

  await loadProfile()
})

async function loadProfile() {
  loading.value = true
  try {
    const data = await profileApi.getMyProfile()
    if (data) {
      form.nickname = data.nickname || ''
      form.avatarUrl = data.avatarUrl || ''
      form.name = data.name || ''
      form.phone = data.phone || ''
      form.wechatId = data.wechatId || ''
      form.major = data.major || ''
      form.grade = data.grade || ''
      form.highSchoolId = data.highSchoolId || null
      if (form.highSchoolId === userStore.userInfo?.highSchoolId) {
        hsSelectedName.value = data.highSchoolName || userStore.userInfo?.highSchoolName || ''
      }
      form.universityId = data.universityId || null
      uniSelectedName.value = data.universityName || userStore.userInfo?.universityName || ''
      gradeIndex.value = gradeOptions.indexOf(form.grade)
    }

    // 兼容此前已注册但本地仍保存旧 Token/旧用户信息的情况。
    if (currentRole.value === 'student' && userStore.userInfo?.relatedId == null) {
      try {
        const studentInfo = await studentApi.getMyInfo()
        if (studentInfo?.id) {
          form.name = studentInfo.name || form.name
          form.phone = studentInfo.phone || form.phone
          form.avatarUrl = studentInfo.avatarUrl || form.avatarUrl
          form.highSchoolId = studentInfo.highSchoolId || form.highSchoolId
          hsSelectedName.value = studentInfo.highSchoolName || hsSelectedName.value
          userStore.updateUserInfo({
            relatedId: studentInfo.id,
            userType: 'student',
            name: studentInfo.name,
            phone: studentInfo.phone,
            highSchoolId: studentInfo.highSchoolId,
            highSchoolName: studentInfo.highSchoolName
          })
          registrationMode.value = false
        }
      } catch (e) {
        // 未找到学生记录即为正常的首次注册状态。
      }
    }
  } catch (e) {
    if (e.statusCode !== 401) {
      console.error('加载资料失败:', e)
    }
    // 后端不可用时从本地 store 读取
    form.nickname = userStore.userInfo?.nickname || userStore.userInfo?.name || ''
    form.avatarUrl = userStore.userInfo?.avatar || ''
    form.name = userStore.userInfo?.name || ''
    form.phone = userStore.userInfo?.phone || ''
    form.major = userStore.userInfo?.major || ''
    form.grade = userStore.userInfo?.grade || ''
  } finally {
    loading.value = false
  }
}

// 头像展示:后端存的是相对路径(/uploads/xxx.jpg),展示时拼 BASE_URL
function resolveAvatarUrl(url) {
  if (!url) return ''
  if (/^(https?|wxfile):\/\//.test(url)) return url
  if (url.startsWith('/')) return BASE_URL + url
  return url
}

function changeAvatar() {
  uniApi.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      uniApi.uploadFile({
        url: BASE_URL + '/api/upload/image',
        filePath: res.tempFilePaths[0],
        name: 'file',
        // 上传接口需登录，uploadFile 不会自动带 token，必须手动携带
        header: { Authorization: `Bearer ${getToken() || ''}` },
        success: (uploadRes) => {
          if (uploadRes.statusCode !== 200) {
            uniApi.showToast({ title: '头像上传失败，请重试', icon: 'none' })
            return
          }
          try {
            const data = JSON.parse(uploadRes.data)
            if (data.code === 200 && data.data) {
              form.avatarUrl = data.data
            } else {
              uniApi.showToast({ title: data.message || '头像上传失败', icon: 'none' })
            }
          } catch (e) {
            console.error('解析上传结果失败:', e)
            uniApi.showToast({ title: '头像上传失败，请重试', icon: 'none' })
          }
        },
        fail: (err) => {
          console.error('头像上传失败:', err)
          uniApi.showToast({ title: '头像上传失败，请重试', icon: 'none' })
        }
      })
    }
  })
}

function onGradeChange(e) {
  gradeIndex.value = e.detail.value
  form.grade = gradeOptions[e.detail.value]
}

function onHighSchoolSelect(school) {
  form.highSchoolId = school.id
  hsSelectedName.value = school.name
}

function onUniversitySelect(university) {
  form.universityId = university.id
  uniSelectedName.value = university.name
}

async function handleSave() {
  if (saving.value) return

  const nick = form.nickname.trim() || userStore.userInfo?.nickname || ''
  const phone = form.phone.trim()
  // 所有角色统一：优先使用用户填写的真实姓名，未填才回退昵称（修复志愿者姓名被静默丢弃）
  const name = form.name?.trim() || nick

  if (!name) {
    uniApi.showToast({ title: '请输入姓名', icon: 'none' })
    return
  }
  if (phone && !/^1\d{10}$/.test(phone)) {
    uniApi.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (isFirstRegistration.value && !phone) {
    uniApi.showToast({ title: '请输入手机号', icon: 'none' })
    return
  }
  if (isFirstRegistration.value && !form.highSchoolId) {
    uniApi.showToast({ title: '请选择所在高中', icon: 'none' })
    return
  }

  saving.value = true
  const registering = isFirstRegistration.value
  uniApi.showLoading({ title: registering ? '注册中...' : '保存中...' })

  const updates = {
    nickname: nick,
    avatarUrl: form.avatarUrl,
    name,
    phone,
    major: form.major?.trim(),
    grade: form.grade,
    wechatId: form.wechatId?.trim()
  }

  // 志愿者可修改就读大学（后端校验：在队伍中时需先退出队伍）
  if (currentRole.value === 'volunteer' && form.universityId) {
    updates.universityId = form.universityId
  }

  try {
    if (registering) {
      const result = await studentApi.studentRegister({
        nickname: nick,
        name,
        phone,
        avatarUrl: form.avatarUrl || '',
        highSchoolId: form.highSchoolId
      })
      userStore.setWechatIdentity({
        ...result,
        nickname: nick,
        avatarUrl: form.avatarUrl,
        highSchoolId: form.highSchoolId,
        highSchoolName: hsSelectedName.value
      }, 'student')
    } else {
      await profileApi.updateProfile(updates)
      userStore.updateUserInfo({ ...updates, avatar: form.avatarUrl })
    }

    uniApi.showToast({ title: registering ? '注册成功' : '保存成功', icon: 'success' })
    setTimeout(() => {
      if (registering) {
        uniApi.reLaunch({ url: '/pages/student/home' })
      } else {
        uniApi.navigateBack()
      }
    }, 500)
  } catch (e) {
    console.error('保存资料失败:', e)
    // 异常机制：失败弹窗展示具体原因（如"您已注册过高中生身份"），不吞错误
    uniApi.showModal({
      title: registering ? '注册失败' : '保存失败',
      content: e.message || '网络异常，请稍后重试',
      showCancel: false,
      confirmText: '知道了'
    })
  } finally {
    uniApi.hideLoading()
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.edit-profile {
  min-height: 100vh; background: #F2F6FB;

  &__loading { display: flex; justify-content: center; padding: 120rpx 0; color: #999; }
  &__body { padding-bottom: 60rpx; }
  &__section { background: #fff; margin-top: 20rpx; }

  &__row {
    display: flex; align-items: center; justify-content: space-between;
    padding: 28rpx 32rpx; border-bottom: 2rpx solid #f5f5f5;
    &:last-child { border-bottom: none; }
  }

  &__row-right { display: flex; align-items: center; gap: 12rpx; }
  &__school-search { flex: 1; min-width: 0; }
  &__label { font-size: 30rpx; color: #333; width: 140rpx; flex-shrink: 0; }
  &__input { flex: 1; font-size: 30rpx; color: #333; text-align: right; }
  &__avatar-img { width: 100rpx; height: 100rpx; border-radius: 50%; background: #f0f0f0; }
  &__value { font-size: 28rpx; color: #5B9BD5; }
  &__arrow { font-size: 36rpx; color: #ccc; }

  &__picker {
    flex: 1; display: flex; align-items: center; justify-content: flex-end;
    &-text { font-size: 30rpx; color: #333; }
    &-placeholder { font-size: 30rpx; color: #ccc; }
  }

  &__tip { font-size: 24rpx; color: #999; padding: 20rpx 32rpx; display: block; }

  &__btn {
    margin: 40rpx 32rpx; height: 88rpx;
    background: linear-gradient(135deg, #6FB1E5, #4A8BC4);
    border-radius: 16rpx;
    display: flex; align-items: center; justify-content: center;
    &--loading { opacity: 0.7; }
    &-text { font-size: 32rpx; color: #fff; font-weight: 600; }
  }
}
</style>
