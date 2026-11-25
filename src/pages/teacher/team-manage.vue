<template>
  <view class="team-manage">
    <NavBar title="组队管理" />

    <view v-if="loading" class="team-manage__loading">
      <text class="team-manage__loading-text">加载中...</text>
    </view>

    <!-- 加载失败（网络异常，非空数据） -->
    <view v-else-if="loadFailed" class="team-manage__load-error">
      <text class="team-manage__load-error-icon">⚠️</text>
      <text class="team-manage__load-error-text">组队信息加载失败：{{ loadError }}</text>
      <button class="team-manage__load-error-btn" @tap="loadData">重新加载</button>
    </view>

    <view v-else class="team-manage__body">
      <!-- 总体统计 -->
      <view class="team-manage__summary">
        <view class="summary-card">
          <text class="summary-card__num">{{ totalTeams }}</text>
          <text class="summary-card__label">已建队伍</text>
        </view>
        <view class="summary-card">
          <text class="summary-card__num">{{ totalGrouped }}</text>
          <text class="summary-card__label">已组队人数</text>
        </view>
        <view class="summary-card summary-card--warn">
          <text class="summary-card__num">{{ totalUngrouped }}</text>
          <text class="summary-card__label">未组队人数</text>
        </view>
      </view>

      <!-- 一键组队 + 自定义建队按钮 -->
      <view class="team-manage__auto-btns">
        <view class="team-manage__auto-btn" @tap="handleAutoBuild">
          <text class="team-manage__auto-btn-icon">🤖</text>
          <text class="team-manage__auto-btn-text">
            一键自动组队<text v-if="totalUngrouped > 0">（{{ totalUngrouped }}人未组队）</text>
            <text v-else>（所有人已组队）</text>
          </text>
        </view>
        <view class="team-manage__manual-btn" @tap="openCreateModal">
          <text class="team-manage__auto-btn-icon">📝</text>
          <text class="team-manage__auto-btn-text">手动创建队伍</text>
        </view>
      </view>

      <!-- 数据为空时的提示 -->
      <view v-if="teamStatus.length === 0" class="team-manage__empty-state">
        <text class="team-manage__empty-state-icon">📭</text>
        <text class="team-manage__empty-state-title">暂无组队数据</text>
        <text class="team-manage__empty-state-desc">请先审核通过志愿者报名，然后即可使用自动组队或手动建队功能</text>
        <view class="team-manage__empty-state-btn" @tap="handleAutoBuild">
          <text class="team-manage__empty-state-btn-text">尝试自动组队</text>
        </view>
      </view>

      <!-- 按高中分组的组队详情 -->
      <view class="team-manage__section" v-for="group in teamStatus" :key="group.highSchoolId">
        <view class="team-manage__section-header">
          <text class="team-manage__section-title">🏫 {{ group.highSchoolName }}</text>
          <view class="team-manage__section-stats">
            <text class="team-manage__section-stat">队伍 {{ group.teamCount }} 支</text>
            <text class="team-manage__section-stat">已组 {{ group.groupedCount }} 人</text>
          </view>
        </view>

        <!-- 队伍列表 -->
        <view v-if="group.teams && group.teams.length > 0" class="team-card-list">
          <view class="team-card" v-for="team in group.teams" :key="team.id" @tap="goToDetail(team)">
            <view class="team-card__header">
              <text class="team-card__name">{{ team.teamName || team.teamCode || '队伍' }}</text>
              <text class="team-card__code" v-if="team.teamCode">{{ team.teamCode }}</text>
            </view>
            <view class="team-card__info">
              <text class="team-card__info-item">👤 队长：{{ team.captainName || '未指定' }}</text>
              <text class="team-card__info-item">👥 成员：{{ team.memberCount || 0 }}人</text>
            </view>
            <view class="team-card__footer">
              <text class="team-card__detail">查看详情 ›</text>
            </view>
          </view>
        </view>
        <view v-else class="team-manage__empty">
          <text class="team-manage__empty-text">该高中暂无队伍</text>
        </view>

        <!-- 未组队人员 -->
        <view v-if="group.ungrouped && group.ungrouped.length > 0" class="ungrouped-list">
          <text class="ungrouped-list__title">⚠️ 未组队人员（{{ group.ungroupedCount }}人）</text>
          <view class="ungrouped-item" v-for="v in group.ungrouped" :key="v.id">
            <view class="ungrouped-item__avatar">
              <text class="ungrouped-item__avatar-text">{{ (v.name || '?')[0] }}</text>
            </view>
            <view class="ungrouped-item__info">
              <text class="ungrouped-item__name">{{ v.name }}</text>
              <text class="ungrouped-item__major">{{ v.major }} · {{ v.grade }}</text>
            </view>
            <view class="ungrouped-item__ops">
              <text class="ungrouped-item__phone" v-if="v.phone">{{ v.phone }}</text>
              <view class="ungrouped-item__join" @tap.stop="handleAddMemberToGroup(v, group)">入队</view>
            </view>
          </view>
        </view>
      </view>

      <!-- 全部已组队 -->
      <view v-if="totalUngrouped === 0 && teamStatus.length > 0" class="team-manage__all-done">
        <text class="team-manage__all-done-icon">✅</text>
        <text class="team-manage__all-done-text">所有志愿者已全部完成组队</text>
      </view>
    </view>

    <!-- 手动创建队伍弹窗 -->
    <view class="team-manage__modal" v-if="showCreateModal">
      <view class="team-manage__modal-mask" @tap="closeCreateModal"></view>
      <view class="team-manage__modal-content" @tap.stop>
        <text class="team-manage__modal-title">手动创建队伍</text>
        <view class="team-manage__modal-field">
          <text class="team-manage__modal-label">队伍名称</text>
          <input class="team-manage__modal-input" v-model="createForm.teamName" placeholder="请输入队伍名称" />
        </view>
        <view class="team-manage__modal-field">
          <text class="team-manage__modal-label">目标高中（支持按省市搜索）</text>
          <HighSchoolSearch
            :selected-id="createForm.highSchoolId"
            :selected-name="createForm.highSchoolName"
            placeholder="搜索并选择目标高中"
            @select="onCreateHighSchoolSelect"
          />
        </view>
        <view class="team-manage__modal-field">
          <text class="team-manage__modal-label">指定队长（选填）</text>
          <picker :range="captainCandidateNames" @change="onCreateCaptainChange">
            <view class="team-manage__picker">
              <text :class="['team-manage__picker-text', !createForm.captainVolunteerId && 'team-manage__picker-text--empty']">
                {{ createForm.captainName || '暂不指定' }}
              </text>
              <text class="team-manage__picker-arrow">›</text>
            </view>
          </picker>
        </view>
        <view class="team-manage__modal-actions">
          <button class="team-manage__modal-btn team-manage__modal-btn--cancel" @tap="closeCreateModal">取消</button>
          <button class="team-manage__modal-btn team-manage__modal-btn--confirm" @tap="handleCreateTeam">创建</button>
        </view>
      </view>
     </view>
  </view>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import HighSchoolSearch from '@/components/HighSchoolSearch.vue'
import * as adminApi from '@/api/admin'

const loading = ref(true)
const loadFailed = ref(false)
const loadError = ref('')
const teamStatus = ref([])

// 手动创建队伍弹窗
const showCreateModal = ref(false)
const createForm = reactive({ teamName: '', highSchoolId: null, highSchoolName: '', captainVolunteerId: null, captainName: '' })

const totalTeams = computed(() => {
  return teamStatus.value.reduce((sum, g) => sum + (g.teamCount || 0), 0)
})
const totalGrouped = computed(() => {
  return teamStatus.value.reduce((sum, g) => sum + (g.groupedCount || 0), 0)
})
const totalUngrouped = computed(() => {
  return teamStatus.value.reduce((sum, g) => sum + (g.ungroupedCount || 0), 0)
})
const captainCandidates = computed(() => {
  const all = []
  teamStatus.value.forEach(g => {
    (g.ungrouped || []).forEach(v => {
      all.push({ id: v.id, name: v.name, group: g })
    })
  })
  return all
})
const captainCandidateNames = computed(() => ['暂不指定', ...captainCandidates.value.map(v => v.name)])

onShow(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  loadFailed.value = false
  loadError.value = ''
  try {
    const data = await adminApi.getTeamStatus()
    teamStatus.value = data || []
  } catch (e) {
    console.error('加载组队状态失败:', e)
    loadFailed.value = true
    loadError.value = e.message || '请求失败'
    teamStatus.value = []
  } finally {
    loading.value = false
  }
}

async function handleAutoBuild() {
  uni.showModal({
    title: '确认自动组队',
    content: `当前有 ${totalUngrouped.value} 人未组队。系统将按高中自动分组，优先选择申请了队长的志愿者作为队长。确认执行？`,
    success: async (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '组队中...' })
      try {
        const result = await adminApi.autoBuildTeams()
        uni.hideLoading()
        uni.showToast({ title: result?.message || '组队完成', icon: 'success' })
        await loadData()
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '自动组队失败', icon: 'none' })
      }
    }
  })
}

// 打开/关闭创建队伍弹窗（关闭时重置表单，避免残留上次输入）
function openCreateModal() {
  resetCreateForm()
  showCreateModal.value = true
}

function closeCreateModal() {
  showCreateModal.value = false
  resetCreateForm()
}

function resetCreateForm() {
  createForm.teamName = ''
  createForm.highSchoolId = null
  createForm.highSchoolName = ''
  createForm.captainVolunteerId = null
  createForm.captainName = ''
}

// 创建队伍目标高中（HighSchoolSearch 选择，支持省市搜索）
function onCreateHighSchoolSelect(school) {
  createForm.highSchoolId = school.id
  createForm.highSchoolName = school.name
}

function onCreateCaptainChange(e) {
  const index = e.detail.value
  if (index === 0) {
    createForm.captainVolunteerId = null
    createForm.captainName = ''
    return
  }
  const c = captainCandidates.value[index - 1]
  if (c) {
    createForm.captainVolunteerId = c.id
    createForm.captainName = c.name
  }
}

async function handleCreateTeam() {
  if (!createForm.teamName.trim()) {
    uni.showToast({ title: '请输入队伍名称', icon: 'none' })
    return
  }
  if (!createForm.highSchoolId) {
    uni.showToast({ title: '请选择目标高中', icon: 'none' })
    return
  }
  uni.showLoading({ title: '创建中...' })
  try {
    await adminApi.createTeacherTeam({
      teamName: createForm.teamName.trim(),
      highSchoolId: createForm.highSchoolId,
      captainVolunteerId: createForm.captainVolunteerId || null
    })
    uni.hideLoading()
    uni.showToast({ title: '队伍创建成功', icon: 'success' })
    closeCreateModal()
    await loadData()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '创建失败', icon: 'none' })
  }
}

// 添加成员：从未组队人员行进入（直接入该高中第一支队伍）
async function handleAddMemberToGroup(v, group) {
  const teams = group.teams || []
  if (teams.length === 0) {
    uni.showToast({ title: '该高中暂无队伍，请先手动创建', icon: 'none' })
    return
  }
  const target = teams[0]
  uni.showModal({
    title: '确认入队',
    content: `将「${v.name}」加入「${target.teamName || target.teamCode}」？`,
    success: async (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '操作中...' })
      try {
        await adminApi.modifyTeamMember(target.id, { volunteerId: v.id, action: 'ADD' })
        uni.hideLoading()
        uni.showToast({ title: '已加入队伍', icon: 'success' })
        await loadData()
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    }
  })
}

// 跳转队伍详情页
function goToDetail(team) {
  uni.navigateTo({ url: `/pages/teacher/team-detail?id=${team.id}` })
}
</script>

<style lang="scss" scoped>
.team-manage {
  min-height: 100vh;
  background: #f0f2f5;

  &__loading {
    display: flex; justify-content: center; padding: 120rpx 0;
    &-text { font-size: 28rpx; color: #a0aec0; }
  }

  // 加载失败
  &__load-error {
    padding: 120rpx 48rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20rpx;

    &-icon { font-size: 72rpx; }
    &-text { font-size: 26rpx; color: #666; text-align: center; }
    &-btn {
      margin-top: 12rpx;
      font-size: 26rpx;
      color: #fff;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 999rpx;
      padding: 0 48rpx;
      height: 72rpx;
      line-height: 72rpx;

      &::after { border: none; }
    }
  }

  &__empty-state {
    background: #fff; border-radius: 20rpx; padding: 80rpx 40rpx;
    display: flex; flex-direction: column; align-items: center; text-align: center;

    &-icon { font-size: 80rpx; margin-bottom: 20rpx; }
    &-title { font-size: 30rpx; font-weight: 600; color: #2d3748; margin-bottom: 12rpx; }
    &-desc { font-size: 24rpx; color: #a0aec0; line-height: 1.6; margin-bottom: 32rpx; }
    &-btn {
      background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 16rpx;
      padding: 20rpx 48rpx; box-shadow: 0 6rpx 20rpx rgba(102,126,234,0.3);
      &-text { font-size: 28rpx; color: #fff; font-weight: 600; }
    }
  }

  &__body {
    padding: 24rpx;
    padding-bottom: 80rpx;
  }

  // 统计
  &__summary {
    display: flex; gap: 16rpx; margin-bottom: 24rpx;
  }

  &__auto-btn {
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 16rpx; padding: 28rpx 24rpx;
    display: flex; align-items: center; justify-content: center; gap: 12rpx;
    margin-bottom: 28rpx;
    box-shadow: 0 6rpx 20rpx rgba(102,126,234,0.3);

    &-icon { font-size: 36rpx; }
    &-text { font-size: 28rpx; color: #fff; font-weight: 600; }
  }

  &__auto-btns {
    display: flex; gap: 16rpx; margin-bottom: 28rpx;
  }

  &__manual-btn {
    background: linear-gradient(135deg, #48bb78, #38a169);
    border-radius: 16rpx; padding: 28rpx 24rpx;
    display: flex; align-items: center; justify-content: center; gap: 12rpx;
    flex: 1;
    box-shadow: 0 6rpx 20rpx rgba(72,187,120,0.3);
  }

  &__auto-btn {
    flex: 1;
    margin-bottom: 0;
  }

  // 分区
  &__section {
    margin-bottom: 28rpx;

    &-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 16rpx;
    }

    &-title { font-size: 30rpx; font-weight: 700; color: #2d3748; }
    &-stats { display: flex; gap: 16rpx; }
    &-stat { font-size: 22rpx; color: #718096; }
  }

  &__empty {
    background: #fff; border-radius: 14rpx; padding: 40rpx; text-align: center; margin-bottom: 16rpx;
    &-text { font-size: 26rpx; color: #a0aec0; }
  }

  &__all-done {
    background: #fff; border-radius: 20rpx; padding: 60rpx 24rpx;
    display: flex; flex-direction: column; align-items: center;
    &-icon { font-size: 64rpx; margin-bottom: 16rpx; }
    &-text { font-size: 28rpx; color: #48bb78; font-weight: 600; }
  }
}

// 统计卡片
.summary-card {
  flex: 1; background: #fff; border-radius: 16rpx; padding: 24rpx 16rpx;
  text-align: center; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
  position: relative; overflow: hidden;

  &::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 6rpx; }
  &::before { background: #5B9BD5; }
  &:nth-child(2)::before { background: #48bb78; }
  &--warn::before { background: #ecc94b; }

  &__num { font-size: 44rpx; font-weight: 800; color: #2d3748; display: block; }
  &__label { font-size: 22rpx; color: #a0aec0; margin-top: 4rpx; display: block; }
}

// 队伍卡片
.team-card-list { margin-bottom: 16rpx; }

.team-card {
  background: #fff; border-radius: 14rpx; padding: 24rpx;
  margin-bottom: 12rpx; box-shadow: 0 2rpx 6rpx rgba(0,0,0,0.03);

  &__header {
    display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx;
  }
  &__name { font-size: 28rpx; font-weight: 600; color: #2d3748; }
  &__code { font-size: 24rpx; color: #5B9BD5; background: rgba(91,155,213,0.12); padding: 4rpx 14rpx; border-radius: 8rpx; }
  &__info { margin-bottom: 12rpx; }
  &__info-item { font-size: 24rpx; color: #718096; display: block; margin-bottom: 4rpx; }

  &__footer {
    display: flex; justify-content: flex-end;
    border-top: 1rpx solid #f5f5f5; padding-top: 16rpx;
  }

  &__detail { font-size: 24rpx; color: #5B9BD5; }
}

// 未组队列表
.ungrouped-list {
  background: #fffbeb; border-radius: 14rpx; padding: 20rpx;
  border: 2rpx solid #fef3c7;

  &__title { font-size: 24rpx; font-weight: 600; color: #d97706; display: block; margin-bottom: 12rpx; }
}

.ungrouped-item {
  display: flex; align-items: center; background: #fff; border-radius: 10rpx; padding: 16rpx; margin-bottom: 8rpx;

  &:last-child { margin-bottom: 0; }

  &__avatar {
    width: 56rpx; height: 56rpx; background: #5B9BD5; border-radius: 14rpx;
    display: flex; align-items: center; justify-content: center; margin-right: 16rpx; flex-shrink: 0;
    &-text { font-size: 24rpx; font-weight: 600; color: #fff; }
  }
  &__info { flex: 1; }
  &__name { font-size: 26rpx; font-weight: 600; color: #2d3748; display: block; }
  &__major { font-size: 22rpx; color: #a0aec0; }
  &__ops { display: flex; align-items: center; gap: 12rpx; flex-shrink: 0; }
  &__phone { font-size: 22rpx; color: #718096; }
  &__join {
    font-size: 22rpx; color: #fff; background: #48bb78;
    padding: 6rpx 20rpx; border-radius: 999rpx;
  }
}

// 弹窗
.team-manage__modal {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  z-index: 999;
  display: flex; align-items: center; justify-content: center;
  padding: 40rpx;

  &-mask {
    position: absolute; top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.5);
  }

  &-content {
    position: relative;
    background: #fff; border-radius: 24rpx; padding: 40rpx;
    width: 100%; max-width: 600rpx;
    display: flex; flex-direction: column; align-items: center;
  }

  &-title { font-size: 32rpx; font-weight: 600; color: #333; display: block; margin-bottom: 28rpx; text-align: center; }
  &-field { margin-bottom: 24rpx; width: 100%; }
  &-label { font-size: 24rpx; color: #999; display: block; margin-bottom: 12rpx; }
  &-input {
    height: 88rpx; background: #f8f9fc; border: 2rpx solid #eef0f6;
    border-radius: 16rpx; padding: 0 24rpx;
    font-size: 28rpx; color: #333; box-sizing: border-box;
  }

  &-picker {
    height: 88rpx; background: #f8f9fc; border: 2rpx solid #eef0f6;
    border-radius: 16rpx; padding: 0 24rpx;
    display: flex; align-items: center; justify-content: space-between;
  }

  &-picker-text {
    font-size: 28rpx; color: #333;
    &--empty { color: #c0c4cc; }
  }

  &-picker-arrow { font-size: 32rpx; color: #c0c4cc; }

  &-actions { display: flex; gap: 20rpx; margin-top: 32rpx; width: 100%; }

  &-btn {
    flex: 1; height: 80rpx; border-radius: 16rpx;
    display: flex; align-items: center; justify-content: center;
    font-size: 28rpx; font-weight: 500; border: none;

    &::after { display: none; }
    &--cancel { background: #f5f5f5; color: #666; }
    &--confirm { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
  }
}
</style>
