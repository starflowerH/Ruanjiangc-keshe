<template>
  <view class="team-detail">
    <NavBar title="队伍详情" />

    <view v-if="loading" class="team-detail__loading">
      <text class="team-detail__loading-text">加载中...</text>
    </view>

    <view v-else-if="loadFailed" class="team-detail__load-error">
      <text class="team-detail__load-error-icon">⚠️</text>
      <text class="team-detail__load-error-text">队伍信息加载失败：{{ loadError }}</text>
      <button class="team-detail__load-error-btn" @tap="loadData">重新加载</button>
    </view>

    <view v-else-if="team" class="team-detail__body">
      <!-- 队伍信息卡 -->
      <view class="team-detail__team-card">
        <view class="team-detail__team-header">
          <text class="team-detail__team-name">{{ team.teamName || team.teamCode || '队伍' }}</text>
          <text class="team-detail__team-code" v-if="team.teamCode">{{ team.teamCode }}</text>
        </view>

        <view class="team-detail__info-row">
          <text class="team-detail__info-label">🏫 目标高中</text>
          <text class="team-detail__info-value">{{ team.highSchoolName || '未设置' }}</text>
          <view class="team-detail__info-edit" @tap="showHSModal = true">修改</view>
        </view>
        <view class="team-detail__info-row">
          <text class="team-detail__info-label">🏛️ 所属大学</text>
          <text class="team-detail__info-value">{{ team.universityName || '-' }}</text>
        </view>
        <view class="team-detail__info-row">
          <text class="team-detail__info-label">👤 队长</text>
          <text class="team-detail__info-value">
            {{ team.captainName || '未指定' }}<text v-if="team.captainPhone" class="team-detail__info-phone">（{{ team.captainPhone }}）</text>
          </text>
        </view>
        <view class="team-detail__info-row">
          <text class="team-detail__info-label">👥 成员</text>
          <text class="team-detail__info-value">{{ team.memberCount || 0 }} 人</text>
        </view>
      </view>

      <!-- 成员列表 -->
      <view class="team-detail__section">
        <text class="team-detail__section-title">队伍成员（点击查看详情）</text>
      </view>

      <view v-if="members.length > 0" class="team-detail__member-list">
        <view class="team-detail__member" v-for="m in members" :key="m.volunteerId">
          <view class="team-detail__member-main" @tap="toggleMember(m.volunteerId)">
            <view class="team-detail__member-avatar" :class="'team-detail__member-avatar--' + (m.role || 'member').toLowerCase()">
              <text class="team-detail__member-avatar-text">{{ (m.name || '?')[0] }}</text>
            </view>
            <view class="team-detail__member-info">
              <view class="team-detail__member-name-row">
                <text class="team-detail__member-name">{{ m.name }}</text>
                <text v-if="m.role === 'CAPTAIN'" class="team-detail__member-tag team-detail__member-tag--captain">队长</text>
              </view>
              <text class="team-detail__member-major">{{ m.major }} · {{ m.grade }}</text>
            </view>
            <text class="team-detail__member-arrow">{{ expandedVolunteerId === m.volunteerId ? '▾' : '▸' }}</text>
          </view>

          <!-- 展开详情 -->
          <view class="team-detail__member-detail" v-if="expandedVolunteerId === m.volunteerId">
            <view class="team-detail__detail-row">
              <text class="team-detail__detail-label">电话</text>
              <text class="team-detail__detail-value">{{ m.phone || '-' }}</text>
            </view>
            <view class="team-detail__detail-row">
              <text class="team-detail__detail-label">大学</text>
              <text class="team-detail__detail-value">{{ m.universityName || '-' }}</text>
            </view>
            <view class="team-detail__detail-row">
              <text class="team-detail__detail-label">专业</text>
              <text class="team-detail__detail-value">{{ m.major || '-' }}</text>
            </view>
            <view class="team-detail__detail-row">
              <text class="team-detail__detail-label">年级</text>
              <text class="team-detail__detail-value">{{ m.grade || '-' }}</text>
            </view>
            <view class="team-detail__detail-actions">
              <view class="team-detail__detail-btn team-detail__detail-btn--danger" @tap="handleRemoveMember(m)">移出队伍</view>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="team-detail__empty">
        <text class="team-detail__empty-text">该队伍暂无成员</text>
      </view>

      <!-- 操作按钮 -->
      <view class="team-detail__actions">
        <button class="team-detail__action-btn" @tap="showAddMemberModal = true">添加成员</button>
        <button class="team-detail__action-btn team-detail__action-btn--warn" @tap="handleAssignCaptain">更换队长</button>
        <button class="team-detail__action-btn team-detail__action-btn--danger" @tap="handleDeleteTeam">解散队伍</button>
      </view>
    </view>

    <!-- 修改目标高中弹窗 -->
    <view class="team-detail__modal" v-if="showHSModal">
      <view class="team-detail__modal-mask" @tap="showHSModal = false"></view>
      <view class="team-detail__modal-content" @tap.stop>
        <text class="team-detail__modal-title">修改目标高中</text>
        <text class="team-detail__modal-hint">修改后队伍成员的所属高中将同步更新</text>
        <HighSchoolSearch
          :selected-id="newHighSchoolId"
          :selected-name="newHighSchoolName"
          placeholder="搜索并选择新目标高中"
          @select="onNewHighSchoolSelect"
        />
        <view class="team-detail__modal-actions">
          <button class="team-detail__modal-btn team-detail__modal-btn--cancel" @tap="showHSModal = false">取消</button>
          <button class="team-detail__modal-btn team-detail__modal-btn--confirm" @tap="handleSaveHighSchool">保存</button>
        </view>
      </view>
    </view>

    <!-- 添加成员弹窗 -->
    <view class="team-detail__modal" v-if="showAddMemberModal">
      <view class="team-detail__modal-mask" @tap="showAddMemberModal = false"></view>
      <view class="team-detail__modal-content" @tap.stop>
        <text class="team-detail__modal-title">添加成员</text>
        <picker :range="addMemberNames" @change="onAddMemberChange">
          <view class="team-detail__picker">
            <text :class="['team-detail__picker-text', !addMemberForm.volunteerId && 'team-detail__picker-text--empty']">
              {{ addMemberForm.name || '请选择未组队志愿者' }}
            </text>
            <text class="team-detail__picker-arrow">›</text>
          </view>
        </picker>
        <view class="team-detail__modal-actions">
          <button class="team-detail__modal-btn team-detail__modal-btn--cancel" @tap="showAddMemberModal = false">取消</button>
          <button class="team-detail__modal-btn team-detail__modal-btn--confirm" @tap="handleConfirmAddMember">加入</button>
        </view>
      </view>
    </view>

    <!-- 更换队长弹窗 -->
    <view class="team-detail__modal" v-if="showCaptainModal">
      <view class="team-detail__modal-mask" @tap="showCaptainModal = false"></view>
      <view class="team-detail__modal-content" @tap.stop>
        <text class="team-detail__modal-title">更换队长</text>
        <picker :range="captainMemberNames" @change="onCaptainMemberChange">
          <view class="team-detail__picker">
            <text :class="['team-detail__picker-text', !captainForm.volunteerId && 'team-detail__picker-text--empty']">
              {{ captainForm.name || '请选择新队长' }}
            </text>
            <text class="team-detail__picker-arrow">›</text>
          </view>
        </picker>
        <view class="team-detail__modal-actions">
          <button class="team-detail__modal-btn team-detail__modal-btn--cancel" @tap="showCaptainModal = false">取消</button>
          <button class="team-detail__modal-btn team-detail__modal-btn--confirm" @tap="handleConfirmCaptain">确认</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import HighSchoolSearch from '@/components/HighSchoolSearch.vue'
import * as adminApi from '@/api/admin'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const teamId = ref(null)
const team = ref(null)
const members = ref([])
const loading = ref(true)
const loadFailed = ref(false)
const loadError = ref('')
const expandedVolunteerId = ref(null)
const ungroupedPool = ref([])

// 修改目标高中
const showHSModal = ref(false)
const newHighSchoolId = ref(null)
const newHighSchoolName = ref('')

// 添加成员
const showAddMemberModal = ref(false)
const addMemberForm = reactive({ volunteerId: null, name: '' })

// 更换队长
const showCaptainModal = ref(false)
const captainForm = reactive({ volunteerId: null, name: '' })

onLoad((options) => {
  teamId.value = options?.id ? Number(options.id) : null
  loadData()
})

async function loadData() {
  if (!teamId.value) return
  loading.value = true
  loadFailed.value = false
  loadError.value = ''
  try {
    const data = await adminApi.getTeamDetail(teamId.value)
    team.value = data
    members.value = data?.members || []
    expandedVolunteerId.value = null
    await loadUngroupedPool()
  } catch (e) {
    console.error('加载队伍详情失败:', e)
    loadFailed.value = true
    loadError.value = e.message || '请求失败'
  } finally {
    loading.value = false
  }
}

// 加载全校未组队人员（添加成员候选）
async function loadUngroupedPool() {
  try {
    const status = await adminApi.getTeamStatus()
    const all = []
    ;(status || []).forEach(g => {
      (g.ungrouped || []).forEach(v => {
        all.push({ id: v.id, name: v.name, major: v.major, grade: v.grade, phone: v.phone })
      })
    })
    ungroupedPool.value = all
  } catch (e) {
    console.error('加载未组队人员失败:', e)
    ungroupedPool.value = []
  }
}

const addMemberNames = computed(() => ungroupedPool.value.map(v => v.name))

function toggleMember(volunteerId) {
  expandedVolunteerId.value = expandedVolunteerId.value === volunteerId ? null : volunteerId
}

// ========== 修改目标高中 ==========
function onNewHighSchoolSelect(school) {
  newHighSchoolId.value = school.id
  newHighSchoolName.value = school.name
}

async function handleSaveHighSchool() {
  if (!newHighSchoolId.value) {
    uni.showToast({ title: '请选择新目标高中', icon: 'none' })
    return
  }
  uni.showLoading({ title: '保存中...' })
  try {
    await adminApi.updateTeamHighSchool(team.value.id, newHighSchoolId.value)
    uni.hideLoading()
    uni.showToast({ title: '目标高中已修改', icon: 'success' })
    showHSModal.value = false
    newHighSchoolId.value = null
    newHighSchoolName.value = ''
    await loadData()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '保存失败', icon: 'none' })
  }
}

// ========== 添加成员 ==========
function onAddMemberChange(e) {
  const index = e.detail.value
  const v = ungroupedPool.value[index]
  if (v) {
    addMemberForm.volunteerId = v.id
    addMemberForm.name = v.name
  }
}

async function handleConfirmAddMember() {
  if (!addMemberForm.volunteerId) return
  uni.showLoading({ title: '操作中...' })
  try {
    await adminApi.modifyTeamMember(team.value.id, { volunteerId: addMemberForm.volunteerId, action: 'ADD' })
    uni.hideLoading()
    uni.showToast({ title: '已加入队伍', icon: 'success' })
    showAddMemberModal.value = false
    addMemberForm.volunteerId = null
    addMemberForm.name = ''
    await loadData()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

// ========== 移出成员 ==========
function handleRemoveMember(m) {
  uni.showModal({
    title: '确认移出',
    content: `将「${m.name}」移出队伍？该操作不可撤销，移出后需重新指定入队。`,
    confirmColor: '#e53e3e',
    success: async (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '操作中...' })
      try {
        await adminApi.modifyTeamMember(team.value.id, { volunteerId: m.volunteerId, action: 'REMOVE' })
        uni.hideLoading()
        uni.showToast({ title: '已移出队伍', icon: 'success' })
        await loadData()
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    }
  })
}

// ========== 更换队长（picker 弹窗，避免 actionSheet 6 项限制） ==========
function handleAssignCaptain() {
  const others = members.value.filter(m => m.volunteerId !== team.value.captainVolunteerId)
  if (others.length === 0) {
    uni.showToast({ title: '队伍没有其他成员，无法更换队长', icon: 'none' })
    return
  }
  captainForm.volunteerId = null
  captainForm.name = ''
  showCaptainModal.value = true
}

const captainMemberNames = computed(() => {
  return members.value
    .filter(m => m.volunteerId !== team.value?.captainVolunteerId)
    .map(m => m.name)
})

function onCaptainMemberChange(e) {
  const index = e.detail.value
  const list = members.value.filter(m => m.volunteerId !== team.value?.captainVolunteerId)
  const target = list[index]
  if (target) {
    captainForm.volunteerId = target.volunteerId
    captainForm.name = target.name
  }
}

async function handleConfirmCaptain() {
  if (!captainForm.volunteerId) return
  uni.showLoading({ title: '操作中...' })
  try {
    await adminApi.assignTeamCaptain(team.value.id, captainForm.volunteerId)
    uni.hideLoading()
    uni.showToast({ title: '队长已更换', icon: 'success' })
    showCaptainModal.value = false
    await loadData()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

// ========== 解散队伍 ==========
function handleDeleteTeam() {
  uni.showModal({
    title: '确认解散',
    content: `确定解散「${team.value.teamName || team.value.teamCode}」吗？队伍成员将回到未组队状态。`,
    confirmColor: '#e53e3e',
    success: async (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '解散中...' })
      try {
        await adminApi.deleteTeacherTeam(team.value.id)
        uni.hideLoading()
        uni.showToast({ title: '队伍已解散', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 800)
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.team-detail {
  min-height: 100vh;
  background: #f0f2f5;
  padding-bottom: 120rpx;

  &__loading {
    display: flex; justify-content: center; padding: 120rpx 0;
    &-text { font-size: 28rpx; color: #a0aec0; }
  }

  &__load-error {
    padding: 120rpx 48rpx;
    display: flex; flex-direction: column; align-items: center; gap: 20rpx;
    &-icon { font-size: 72rpx; }
    &-text { font-size: 26rpx; color: #666; text-align: center; }
    &-btn {
      margin-top: 12rpx; font-size: 26rpx; color: #fff;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 999rpx; padding: 0 48rpx; height: 72rpx; line-height: 72rpx;
      &::after { border: none; }
    }
  }

  &__body { padding: 24rpx; }

  // 队伍信息卡
  &__team-card {
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 20rpx; padding: 32rpx;
    color: #fff; margin-bottom: 28rpx;
    box-shadow: 0 8rpx 24rpx rgba(102,126,234,0.3);
  }

  &__team-header {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 24rpx;
  }

  &__team-name { font-size: 38rpx; font-weight: 700; color: #fff; }
  &__team-code {
    font-size: 24rpx; color: #fff; background: rgba(255,255,255,0.2);
    padding: 6rpx 16rpx; border-radius: 8rpx; letter-spacing: 2rpx;
  }

  &__info-row {
    display: flex; align-items: center; padding: 10rpx 0;
  }

  &__info-label { font-size: 26rpx; color: rgba(255,255,255,0.75); width: 160rpx; flex-shrink: 0; }
  &__info-value { font-size: 28rpx; color: #fff; flex: 1; }
  &__info-phone { font-size: 24rpx; color: rgba(255,255,255,0.8); }
  &__info-edit {
    font-size: 24rpx; color: #fff; background: rgba(255,255,255,0.2);
    padding: 6rpx 20rpx; border-radius: 999rpx; flex-shrink: 0;
  }

  // 成员列表
  &__section { margin-bottom: 16rpx; }
  &__section-title { font-size: 30rpx; font-weight: 600; color: #2d3748; }

  &__member-list {
    background: #fff; border-radius: 16rpx; overflow: hidden;
    margin-bottom: 32rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
  }

  &__member {
    border-bottom: 1rpx solid #f5f5f5;
    &:last-child { border-bottom: none; }
  }

  &__member-main {
    display: flex; align-items: center; padding: 24rpx 28rpx;
  }

  &__member-avatar {
    width: 80rpx; height: 80rpx; border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    margin-right: 20rpx; flex-shrink: 0;
    &--captain { background: linear-gradient(135deg, #f59e0b, #d97706); }
    &--member { background: linear-gradient(135deg, #94a3b8, #64748b); }
  }

  &__member-avatar-text { font-size: 32rpx; font-weight: 600; color: #fff; }
  &__member-info { flex: 1; min-width: 0; }
  &__member-name-row { display: flex; align-items: center; gap: 10rpx; margin-bottom: 6rpx; }
  &__member-name { font-size: 30rpx; font-weight: 600; color: #2d3748; }
  &__member-tag {
    font-size: 18rpx; padding: 2rpx 10rpx; border-radius: 6rpx; font-weight: 500;
    &--captain { background: #fef3c7; color: #d97706; }
    &--vice { background: #ede9fe; color: #5B9BD5; }
  }
  &__member-major { font-size: 24rpx; color: #718096; display: block; }
  &__member-arrow { font-size: 28rpx; color: #a0aec0; flex-shrink: 0; margin-left: 12rpx; }

  // 展开详情
  &__member-detail {
    background: #f8fafc; padding: 20rpx 28rpx 24rpx; margin: 0 28rpx 24rpx;
    border-radius: 12rpx;
  }

  &__detail-row {
    display: flex; padding: 8rpx 0;
  }

  &__detail-label { font-size: 24rpx; color: #a0aec0; width: 140rpx; flex-shrink: 0; }
  &__detail-value { font-size: 26rpx; color: #2d3748; flex: 1; }
  &__detail-actions { margin-top: 12rpx; display: flex; justify-content: flex-end; }
  &__detail-btn {
    font-size: 24rpx; padding: 8rpx 28rpx; border-radius: 999rpx;
    &--danger { color: #e53e3e; background: #fff; border: 2rpx solid #e53e3e; }
  }

  &__empty {
    background: #fff; border-radius: 14rpx; padding: 60rpx;
    text-align: center; margin-bottom: 32rpx;
    &-text { font-size: 26rpx; color: #a0aec0; }
  }

  // 操作按钮
  &__actions { display: flex; flex-direction: column; gap: 20rpx; }

  &__action-btn {
    width: 100%; height: 88rpx;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 16rpx; color: #fff; font-size: 28rpx; font-weight: 600;
    display: flex; align-items: center; justify-content: center; border: none;

    &--warn { background: linear-gradient(135deg, #f59e0b, #d97706); }
    &--danger { background: #fff; color: #e53e3e; border: 2rpx solid #e53e3e; }
    &::after { display: none; }
  }

  // 弹窗
  &__modal {
    position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 999;
    display: flex; align-items: center; justify-content: center; padding: 40rpx;
  }

  &__modal-mask {
    position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5);
  }

  &__modal-content {
    position: relative; background: #fff; border-radius: 24rpx; padding: 40rpx;
    width: 100%; max-width: 620rpx;
    display: flex; flex-direction: column;
  }

  &__modal-title { font-size: 32rpx; font-weight: 600; color: #333; margin-bottom: 8rpx; text-align: center; }
  &__modal-hint { font-size: 24rpx; color: #999; margin-bottom: 24rpx; text-align: center; display: block; }

  &__picker {
    height: 88rpx; background: #f8f9fc; border: 2rpx solid #eef0f6;
    border-radius: 16rpx; padding: 0 24rpx;
    display: flex; align-items: center; justify-content: space-between;
  }

  &__picker-text {
    font-size: 28rpx; color: #333;
    &--empty { color: #c0c4cc; }
  }

  &__picker-arrow { font-size: 32rpx; color: #c0c4cc; }

  &__modal-actions { display: flex; gap: 20rpx; margin-top: 32rpx; width: 100%; }

  &__modal-btn {
    flex: 1; height: 80rpx; border-radius: 16rpx;
    display: flex; align-items: center; justify-content: center;
    font-size: 28rpx; font-weight: 500; border: none;
    &::after { display: none; }
    &--cancel { background: #f5f5f5; color: #666; }
    &--confirm { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
  }
}
</style>
