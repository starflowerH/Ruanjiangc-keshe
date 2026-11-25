<template>
  <view class="teacher-audit">
    <NavBar title="老师审核" />

    <!-- 待启用数提示条 -->
    <view class="teacher-audit__hint" v-if="pendingCount > 0">
      <text class="teacher-audit__hint-text">有 {{ pendingCount }} 位老师待启用审核</text>
    </view>

    <!-- 老师列表 -->
    <view class="teacher-audit__list">
      <view class="teacher-audit__card" v-for="item in list" :key="item.id"
        :class="{ 'teacher-audit__card--pending': item.status === 0 }">
        <view class="teacher-audit__head">
          <view class="teacher-audit__name-row">
            <text class="teacher-audit__name">{{ item.name || '未命名老师' }}</text>
            <text :class="['teacher-audit__badge', item.status === 1 ? 'teacher-audit__badge--on' : 'teacher-audit__badge--off']">
              {{ item.status === 1 ? '已启用' : '待启用' }}
            </text>
          </view>
          <text class="teacher-audit__time">{{ formatTime(item.createdAt) }}</text>
        </view>
        <view class="teacher-audit__info">
          <text class="teacher-audit__info-item">大学ID: {{ item.universityId || '-' }}</text>
          <text class="teacher-audit__info-item" v-if="item.phone">手机: {{ item.phone }}</text>
        </view>
        <view class="teacher-audit__permissions">
          <view class="teacher-audit__permission-head">
            <text class="teacher-audit__permission-title">业务权限</text>
            <text class="teacher-audit__permission-count">{{ permissionCount(item) }}/4 已下发</text>
          </view>
          <view class="teacher-audit__permission" v-for="permission in permissionOptions" :key="permission.key">
            <view class="teacher-audit__permission-copy">
              <text class="teacher-audit__permission-name">{{ permission.label }}</text>
              <text class="teacher-audit__permission-desc">{{ permission.description }}</text>
            </view>
            <switch
              color="#2563eb"
              :checked="hasPermission(item, permission.key)"
              :disabled="savingId === item.id"
              @change="changePermission(item, permission.key, $event.detail.value)"
            />
          </view>
        </view>
        <view class="teacher-audit__actions">
          <button v-if="item.status === 0" class="teacher-audit__btn teacher-audit__btn--enable" @tap="toggle(item, true)">启用</button>
          <button v-else class="teacher-audit__btn teacher-audit__btn--disable" @tap="toggle(item, false)">禁用</button>
        </view>
      </view>
      <view class="teacher-audit__empty" v-if="!loading && list.length === 0">
        <text class="teacher-audit__empty-text">暂无老师账号</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import { formatTime } from '@/utils/format'
import * as adminApi from '@/api/admin'

const list = ref([])
const loading = ref(false)
const savingId = ref(null)
const permissionOptions = [
  { key: 'teacherVolunteerAudit', label: '报名审核', description: '审核、取消本校大学生志愿者资格' },
  { key: 'teacherTeamManage', label: '组队管理', description: '自动组队、建队及调整队伍成员' },
  { key: 'teacherPromotionManage', label: '宣传资料', description: '编辑本校介绍、图片和招生资料' },
  { key: 'teacherForumModeration', label: '论坛治理', description: '审核本校帖子、举报、申诉和精华' }
]
const pendingCount = computed(() => list.value.filter(i => i.status === 0).length)

onShow(load)
async function load() {
  loading.value = true
  try {
    const data = await adminApi.getTeacherList()
    list.value = (data || []).sort((a, b) => (a.status === 0 ? -1 : 1) - (b.status === 0 ? -1 : 1))
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function parsePermissions(item) {
  if (!item.permissions) {
    return Object.fromEntries(permissionOptions.map(option => [option.key, true]))
  }
  try {
    return typeof item.permissions === 'string' ? JSON.parse(item.permissions) : item.permissions
  } catch {
    return {}
  }
}

function hasPermission(item, key) {
  return parsePermissions(item)[key] === true
}

function permissionCount(item) {
  return permissionOptions.filter(option => hasPermission(item, option.key)).length
}

async function changePermission(item, key, enabled) {
  const previous = parsePermissions(item)
  const next = Object.fromEntries(permissionOptions.map(option => [
    option.key,
    option.key === key ? enabled : previous[option.key] === true
  ]))
  savingId.value = item.id
  item.permissions = next
  try {
    await adminApi.updateTeacherPermissions(item.id, next)
    uni.showToast({ title: enabled ? '权限已下发' : '权限已撤销', icon: 'success' })
  } catch (e) {
    item.permissions = previous
    uni.showToast({ title: e.message || '权限更新失败', icon: 'none' })
  } finally {
    savingId.value = null
  }
}

function toggle(item, enabled) {
  uni.showModal({
    title: enabled ? '启用老师' : '禁用老师',
    content: `确认${enabled ? '启用' : '禁用'}「${item.name || '该老师'}」？${enabled ? '启用后该老师可登录老师端' : '禁用后其账号将立即无法访问'}`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await adminApi.enableTeacher(item.id, enabled)
        uni.showToast({ title: enabled ? '已启用' : '已禁用', icon: 'success' })
        load()
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.teacher-audit { min-height: 100vh; background: #f3f6f9; padding-bottom: 40rpx; }
.teacher-audit__hint { margin: 20rpx 24rpx 0; padding: 20rpx 24rpx; background: #fef3c7; border-radius: 12rpx; }
.teacher-audit__hint-text { font-size: 26rpx; color: #92400e; }
.teacher-audit__card { margin: 20rpx 24rpx; padding: 24rpx; background: #fff; border-radius: 12rpx; border-left: 8rpx solid #d1d5db; }
.teacher-audit__card--pending { border-left-color: #f59e0b; }
.teacher-audit__head { display: flex; justify-content: space-between; align-items: center; }
.teacher-audit__name-row { display: flex; align-items: center; gap: 12rpx; }
.teacher-audit__name { font-size: 30rpx; font-weight: 600; color: #1f2937; }
.teacher-audit__badge { font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 20rpx; }
.teacher-audit__badge--on { color: #15803d; background: #dcfce7; }
.teacher-audit__badge--off { color: #b45309; background: #fef3c7; }
.teacher-audit__time { font-size: 22rpx; color: #9ca3af; }
.teacher-audit__info { display: flex; gap: 24rpx; margin-top: 12rpx; }
.teacher-audit__info-item { font-size: 24rpx; color: #6b7280; }
.teacher-audit__permissions { margin-top: 20rpx; padding-top: 18rpx; border-top: 2rpx solid #eef2f7; }
.teacher-audit__permission-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8rpx; }
.teacher-audit__permission-title { font-size: 26rpx; font-weight: 600; color: #334155; }
.teacher-audit__permission-count { font-size: 22rpx; color: #64748b; }
.teacher-audit__permission { display: flex; align-items: center; min-height: 92rpx; }
.teacher-audit__permission-copy { flex: 1; min-width: 0; padding-right: 16rpx; }
.teacher-audit__permission-name { display: block; font-size: 25rpx; color: #1f2937; }
.teacher-audit__permission-desc { display: block; margin-top: 4rpx; font-size: 21rpx; color: #94a3b8; line-height: 1.4; }
.teacher-audit__actions { display: flex; justify-content: flex-end; margin-top: 16rpx; }
.teacher-audit__btn { margin: 0; width: 160rpx; height: 64rpx; line-height: 64rpx; font-size: 26rpx; border-radius: 8rpx; }
.teacher-audit__btn--enable { color: #fff; background: #1d4ed8; }
.teacher-audit__btn--disable { color: #b91c1c; background: #fee2e2; }
.teacher-audit__empty { padding: 120rpx 0; text-align: center; color: #94a3b8; }
</style>
