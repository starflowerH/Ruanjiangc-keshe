<template>
  <view class="audit-list">
    <NavBar :title="mode === 'qa' ? '问答审核' : '审核管理'" />
    <!-- 城市分块筛选（仅志愿者模式, 暂限广东） -->
    <scroll-view
      v-if="mode !== 'qa' && !userStore.isTeacher && cities.length > 0"
      class="audit-list__city-bar"
      scroll-x
      :show-scrollbar="false"
    >
      <view class="audit-list__city-bar-inner">
        <view
          v-for="c in cities"
          :key="c"
          :class="['city-chip', currentCity === c && 'city-chip--active']"
          @tap="switchCity(c)"
        >
          <text class="city-chip__text">{{ c === 'ALL' ? '全部城市' : c }}</text>
        </view>
      </view>
    </scroll-view>
    <!-- 搜索框（志愿者模式） -->
    <view class="audit-list__search" v-if="mode !== 'qa'">
      <view class="audit-list__search-bar">
        <text class="audit-list__search-icon">🔍</text>
        <input
          class="audit-list__search-input"
          v-model="keyword"
          placeholder="按姓名/手机号/大学搜索"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <text v-if="keyword" class="audit-list__search-clear" @tap="clearSearch">×</text>
      </view>
    </view>
    <!-- 状态筛选（仅志愿者模式） -->
    <view class="audit-list__tabs" v-if="mode !== 'qa'">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab', currentTab === tab.value && 'tab--active']"
        @tap="switchTab(tab.value)"
      >
        <text class="tab__text">{{ tab.label }}</text>
      </view>
    </view>

    <!-- 列表 -->
    <view class="audit-list__content">
      <!-- 问答审核卡片 -->
      <view class="audit-card" v-for="item in list" :key="item.id" v-if="mode === 'qa'">
        <view class="audit-card__header">
          <text class="audit-card__name">{{ item.title || '未命名问答' }}</text>
          <text class="audit-card__status">{{ item.category || '' }}</text>
        </view>
        <view class="audit-card__info">
          <text class="audit-card__info-item">🙋 {{ item.askerName || '匿名' }}</text>
          <text class="audit-card__info-item">🏫 {{ item.askerHighSchool || '' }}</text>
          <text class="audit-card__info-item">⏰ {{ item.askedAt ? String(item.askedAt).slice(0, 10) : '' }}</text>
        </view>
        <view class="audit-card__question">
          <text class="audit-card__question-text">{{ item.questionContent || '' }}</text>
        </view>
        <view class="audit-card__actions" v-if="canAudit">
          <view class="audit-btn audit-btn--reject" @tap="handleQaAudit(item.id, 'REJECTED')">
            <text class="audit-btn__text">拒绝</text>
          </view>
          <view class="audit-btn audit-btn--approve" @tap="handleQaAudit(item.id, 'APPROVED')">
            <text class="audit-btn__text">通过</text>
          </view>
        </view>
      </view>

      <!-- 志愿者/队伍角色申请 混合审核卡片 -->
      <view class="audit-card" v-for="item in list" :key="item._type === 'teamApply' ? 'team-' + item.id : 'vol-' + item.id" v-if="mode !== 'qa'">
        <!-- 队伍角色申请 -->
        <template v-if="item._type === 'teamApply'">
          <view class="audit-card__header">
            <text class="audit-card__name">{{ item.volunteerName || '未知' }}</text>
            <text :class="['audit-card__status', 'audit-card__status--pending', 'audit-card__role-tag', item._roleClass]">
              {{ item._roleText }}
            </text>
          </view>
          <view class="audit-card__info">
            <text class="audit-card__info-item">🏷️ 队伍：{{ item.teamName || '-' }}（{{ item.teamCode || '-' }}）</text>
            <text class="audit-card__info-item">📚 {{ item.volunteerMajor || '' }} {{ item.volunteerGrade || '' }}</text>
            <text class="audit-card__info-item" v-if="item.reason">📝 理由：{{ getTeamDisplayReason(item) }}</text>
          </view>
          <view class="audit-card__actions" v-if="canAudit">
            <view class="audit-btn audit-btn--reject" @tap="handleTeamApplyAudit(item.id, 'REJECTED')">
              <text class="audit-btn__text">拒绝</text>
            </view>
            <view class="audit-btn audit-btn--approve" @tap="handleTeamApplyAudit(item.id, 'APPROVED')">
              <text class="audit-btn__text">通过</text>
            </view>
          </view>
        </template>
        <!-- 志愿者审核 -->
        <template v-else>
          <view class="audit-card__header">
            <text class="audit-card__name">{{ item.name }}</text>
            <text :class="['audit-card__status', `audit-card__status--${item.status?.toLowerCase()}`]">
              {{ statusText(item.status) }}
            </text>
          </view>
          <view class="audit-card__info">
            <text class="audit-card__info-item">📱 {{ item.phone || '未填写' }}</text>
            <text class="audit-card__info-item">🏫 {{ item.universityName || '' }}</text>
            <text class="audit-card__info-item">📚 {{ item.major || '' }} {{ item.grade || '' }}</text>
            <text class="audit-card__info-item">🎯 目标：{{ item.targetHighSchoolName || '未选择' }}</text>
          </view>
          <view class="audit-card__actions" v-if="item.status === 'PENDING' && canAudit">
            <view class="audit-btn audit-btn--reject" @tap="handleAudit(item.id, 'REJECTED')">
              <text class="audit-btn__text">拒绝</text>
            </view>
            <view class="audit-btn audit-btn--approve" @tap="handleAudit(item.id, 'APPROVED')">
              <text class="audit-btn__text">通过</text>
            </view>
          </view>
          <!-- 管理目标高中按钮（已通过的志愿者） -->
          <view class="audit-card__hs-actions" v-if="item.status === 'APPROVED'">
            <view class="hs-btn" v-if="canAudit" @tap="openHighSchoolModal(item)">
              <text class="hs-btn__text">🏫 管理目标高中</text>
            </view>
            <view class="hs-btn hs-btn--danger" v-if="canAudit" @tap="handleCancel(item)">
              <text class="hs-btn__text">取消资格</text>
            </view>
            <view
              class="hs-btn"
              :class="item.forumModeratorGranted && 'hs-btn--granted'"
              v-if="canGrantForumModerator"
              @tap="manageForumModerator(item)"
            >
              <text class="hs-btn__text">{{ item.forumModeratorGranted ? '✓ 审核员已授权' : '论坛审核员' }}</text>
            </view>
            <view
              class="hs-btn"
              :class="item.promotionManagerGranted && 'hs-btn--granted'"
              v-if="canGrantPromotionManager"
              @tap="managePromotionManager(item)"
            >
              <text class="hs-btn__text">{{ item.promotionManagerGranted ? '✓ 宣传已授权' : '宣传资料管理员' }}</text>
            </view>
          </view>
          <!-- 撤销取消按钮（已取消且在2天时限内） -->
          <view class="audit-card__hs-actions" v-if="item.status === 'CANCELLED' && item.canUndo && canAudit">
            <view class="audit-card__cancel-info" v-if="item.cancelledAt">
              <text class="audit-card__cancel-info-text">取消于 {{ String(item.cancelledAt).slice(0, 10) }}，2 天内可撤销</text>
            </view>
            <view class="hs-btn hs-btn--warn" @tap="handleUndoCancel(item)">
              <text class="hs-btn__text">撤销取消</text>
            </view>
          </view>
        </template>
      </view>

      <view class="empty" v-if="list.length === 0">
        <text class="empty__text">暂无数据</text>
      </view>
    </view>

    <!-- 目标高中分配弹窗 -->
    <view class="hs-modal-mask" v-if="showHSModal" @tap="closeHSModal">
      <view class="hs-modal" @tap.stop="">
        <view class="hs-modal__header">
          <text class="hs-modal__title">管理目标高中 - {{ currentVolunteer?.name }}</text>
          <view class="hs-modal__close" @tap="closeHSModal">
            <text class="hs-modal__close-text">✕</text>
          </view>
        </view>
        <view class="hs-modal__body">
          <text class="hs-modal__hint">选择该志愿者要前往宣传的高中（可多选）</text>
          <view class="hs-modal__loading" v-if="hsLoading">
            <text class="hs-modal__loading-text">加载中...</text>
          </view>
          <view v-else class="hs-checkbox-list">
            <view
              v-for="hs in highSchoolOptions"
              :key="hs.id"
              :class="['hs-checkbox-item', selectedHSIds.includes(hs.id) && 'hs-checkbox-item--checked']"
              @tap="toggleHS(hs.id)"
            >
              <view :class="['hs-checkbox', selectedHSIds.includes(hs.id) && 'hs-checkbox--on']">
                <text v-if="selectedHSIds.includes(hs.id)" class="hs-checkbox__icon">✓</text>
              </view>
              <text class="hs-checkbox-item__name">{{ hs.name }}</text>
              <text class="hs-checkbox-item__region" v-if="hs.city">{{ hs.province }} {{ hs.city }}</text>
            </view>
            <view class="hs-empty" v-if="highSchoolOptions.length === 0">
              <text class="hs-empty__text">暂无高中数据</text>
            </view>
          </view>
        </view>
        <view class="hs-modal__footer">
          <view class="hs-modal-btn hs-modal-btn--cancel" @tap="closeHSModal">
            <text class="hs-modal-btn__text">取消</text>
          </view>
          <view class="hs-modal-btn hs-modal-btn--save" @tap="saveHighSchools">
            <text class="hs-modal-btn__text">保存</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import NavBar from '@/components/NavBar.vue'
import * as adminApi from '@/api/admin'
import * as universityApi from '@/api/university'

const userStore = useUserStore()

const tabs = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '全部', value: '' }
]

const currentTab = ref('PENDING')
const list = ref([])
// 审核模式: volunteer=志愿者报名(默认), qa=问答审核
const mode = ref('volunteer')
// 搜索关键词
const keyword = ref('')
// 城市分块（暂限广东）
const cities = ref(['ALL'])
const currentCity = ref('ALL')

const canAudit = computed(() =>
  userStore.isSuperAdmin ||
  (userStore.isTeacher && userStore.hasPermission('teacherVolunteerAudit'))
)
const canGrantForumModerator = computed(() =>
  userStore.isSuperAdmin ||
  (userStore.isTeacher && userStore.hasPermission('teacherForumModeration'))
)
const canGrantPromotionManager = computed(() =>
  userStore.isSuperAdmin ||
  (userStore.isTeacher && userStore.hasPermission('teacherPromotionManage'))
)

// 加载广东大学列表, 提取城市分块(非老师角色才显示, 老师只看本校)
async function loadCities() {
  try {
    const uniData = await universityApi.getUniversityList()
    const list = Array.isArray(uniData) ? uniData : (uniData?.records || [])
    const gd = list.filter(u => u.province === '广东' && u.city)
    const citySet = [...new Set(gd.map(u => u.city))].sort()
    cities.value = ['ALL', ...citySet]
  } catch (e) {
    console.error('加载城市列表失败:', e)
  }
}

function switchCity(c) {
  currentCity.value = c
  loadList()
}

// 支持 URL 参数指定模式和初始 tab
// 统计卡片跳转: ?status=APPROVED / ?status= (全部) / ?type=qa (问答审核)
onLoad((options) => {
  if (options && options.type === 'qa') {
    mode.value = 'qa'
    currentTab.value = ''  // 问答审核只看待审核列表
  }
  if (options && options.status !== undefined) {
    currentTab.value = options.status || ''
  }
})

onShow(async () => {
  if (mode.value !== 'qa') {
    loadCities()
  }
  await loadList()
})

async function switchTab(tab) {
  currentTab.value = tab
  await loadList()
}

// 队伍申请角色文本
function getTeamRoleText(role) {
  if (role === 'CAPTAIN') return '申请队长'
  if (role === 'CAPTAIN_TRANSFER') return '队长转让'
  return role || ''
}

// 队伍申请角色样式类
function getTeamRoleClass(role) {
  if (role === 'CAPTAIN') return 'audit-card__role--captain'
  if (role === 'CAPTAIN_TRANSFER') return 'audit-card__role--transfer'
  return ''
}

// 队伍申请显示理由（队长转让理由格式：转让给:xxx;理由）
function getTeamDisplayReason(item) {
  if (item.applyRole === 'CAPTAIN_TRANSFER' && item.reason) {
    const parts = item.reason.split(';')
    const reasonPart = parts.length > 1 ? parts[1] : ''
    return reasonPart || '无理由'
  }
  return item.reason || ''
}

// 审核队伍角色申请
function handleTeamApplyAudit(id, action) {
  const actionText = action === 'APPROVED' ? '通过' : '拒绝'
  uni.showModal({
    title: '确认审核',
    content: `确定${actionText}该队伍角色申请吗？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        uni.showLoading({ title: '审核中...' })
        await adminApi.auditTeamApplication(id, { action })
        uni.hideLoading()
        uni.showToast({ title: `已${actionText}`, icon: 'success' })
        loadList()
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '审核失败', icon: 'none' })
      }
    }
  })
}

function handleSearch() {
  loadList()
}

function clearSearch() {
  keyword.value = ''
  loadList()
}

async function loadList() {
  uni.showLoading({ title: '加载中...' })
  try {
    if (mode.value === 'qa') {
      // 问答审核模式：拉取待审核问答
      const data = await adminApi.getPendingQA({ page: 1, size: 50 })
      list.value = data?.records || []
      uni.hideLoading()
      return
    }
    const params = { page: 1, size: 50 }
    if (currentTab.value) {
      params.status = currentTab.value
    }
    if (currentCity.value !== 'ALL') {
      params.city = currentCity.value
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim()
    }

    const data = userStore.isTeacher
      ? await adminApi.getTeacherVolunteerList(params)
      : await adminApi.getVolunteerList(params)

    // 统一标记志愿者类型并过滤空项，避免渲染时访问 undefined._type
    list.value = (data?.records || []).filter(Boolean).map(item => ({ ...item, _type: 'volunteer' }))

    // 待审核 tab + 超级管理员：合并队伍角色申请（队长申请/转让）
    if (currentTab.value === 'PENDING' && userStore.isSuperAdmin) {
      try {
        const apps = await adminApi.getTeamApplications()
        const appsArr = Array.isArray(apps) ? apps : (apps?.records || [])
        const teamApplies = appsArr.filter(Boolean).map(app => ({
          ...app,
          _type: 'teamApply',
          _roleText: getTeamRoleText(app.applyRole),
          _roleClass: getTeamRoleClass(app.applyRole)
        }))
        // 队伍申请合并到列表，队伍申请排在前面便于优先处理
        list.value = [...teamApplies, ...list.value]
      } catch (e) {
        console.error('加载队伍申请失败:', e)
      }
    }
  } catch (e) {
    console.error('加载列表失败:', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function statusText(status) {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CANCELLED: '已取消',
    REPLACED: '已重新报名'
  }
  return map[status] || status
}

// ========== 目标高中分配管理 ==========
const showHSModal = ref(false)
const currentVolunteer = ref(null)
const selectedHSIds = ref([])
const highSchoolOptions = ref([])
const hsLoading = ref(false)

async function openHighSchoolModal(item) {
  currentVolunteer.value = item
  selectedHSIds.value = []
  showHSModal.value = true
  hsLoading.value = true

  try {
    // 并行加载高中列表和当前志愿者的高中绑定
    const [hsList, currentHS] = await Promise.all([
      adminApi.getHighSchoolList().catch(() => []),
      adminApi.getVolunteerHighSchools(item.id, userStore.isTeacher).catch(() => [])
    ])
    highSchoolOptions.value = Array.isArray(hsList) ? hsList : (hsList?.records || [])
    // 当前绑定的高中ID（返回的是高中对象数组或ID数组）
    if (Array.isArray(currentHS)) {
      selectedHSIds.value = currentHS.map(h => h.id || h).filter(Boolean)
    }
  } catch (e) {
    console.error('加载高中数据失败:', e)
    uni.showToast({ title: '加载高中列表失败', icon: 'none' })
  } finally {
    hsLoading.value = false
  }
}

function closeHSModal() {
  showHSModal.value = false
  currentVolunteer.value = null
  selectedHSIds.value = []
}

function toggleHS(id) {
  const idx = selectedHSIds.value.indexOf(id)
  if (idx >= 0) {
    selectedHSIds.value.splice(idx, 1)
  } else {
    selectedHSIds.value.push(id)
  }
}

async function saveHighSchools() {
  if (!currentVolunteer.value) return
  uni.showLoading({ title: '保存中...' })
  try {
    await adminApi.assignVolunteerHighSchools(
      currentVolunteer.value.id,
      selectedHSIds.value,
      userStore.isTeacher
    )
    uni.hideLoading()
    uni.showToast({ title: '高中分配已更新', icon: 'success' })
    closeHSModal()
    await loadList()
  } catch (e) {
    uni.hideLoading()
    console.error('保存高中分配失败:', e)
    uni.showToast({ title: '保存失败: ' + (e.message || '请重试'), icon: 'none' })
  }
}

async function handleAudit(id, status) {
  const action = status === 'APPROVED' ? '通过' : '拒绝'
  uni.showModal({
    title: '确认操作',
    content: `确定要${action}该报名吗？`,
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '处理中...' })
        try {
          await adminApi.auditVolunteer({ id, status }, userStore.isTeacher)
          uni.hideLoading()
          uni.showToast({ title: `${action}成功`, icon: 'success' })
          await loadList()
        } catch (e) {
          uni.hideLoading()
          uni.showToast({ title: `${action}失败`, icon: 'none' })
        }
      }
    }
  })
}

function manageForumModerator(item) {
  const granted = !!item.forumModeratorGranted
  // 按当前状态生成选项：未授权→授权为主；已授权→撤销为主
  const itemList = granted ? ['撤销审核权限', '取消'] : ['授权为本校审核员', '取消']
  uni.showActionSheet({
    title: granted ? '当前已是本校审核员' : '当前未授权审核权限',
    itemList,
    success: async (res) => {
      if (res.tapIndex === 1) return // 取消
      const active = !granted
      try {
        await adminApi.setForumModerator(item.id, active, userStore.isTeacher)
        uni.showToast({ title: active ? '授权成功' : '已撤销权限', icon: 'success' })
        await loadList()
      } catch (e) { uni.showToast({ title: e.message || '操作失败', icon: 'none' }) }
    }
  })
}

function managePromotionManager(item) {
  const granted = !!item.promotionManagerGranted
  const itemList = granted ? ['撤销宣传资料权限', '取消'] : ['授权管理本校宣传资料', '取消']
  uni.showActionSheet({
    title: granted ? '当前已是宣传资料管理员' : '当前未授权宣传权限',
    itemList,
    success: async (res) => {
      if (res.tapIndex === 1) return // 取消
      const active = !granted
      try {
        await adminApi.setPromotionManager(item.id, active, userStore.isTeacher)
        uni.showToast({ title: active ? '授权成功' : '已撤销权限', icon: 'success' })
        await loadList()
      } catch (e) { uni.showToast({ title: e.message || '操作失败', icon: 'none' }) }
    }
  })
}

// ========== 取消资格 / 撤销取消 ==========
function handleCancel(item) {
  const modalContent = '确认取消「' + item.name + '」的志愿者资格吗？\n取消后将移出其所在队伍，且无法再回答新问题。'
  uni.showModal({
    title: '取消资格',
    content: modalContent,
    editable: true,
    placeholderText: '请输入取消原因（选填）',
    confirmText: '确认取消',
    cancelText: '再想想',
    success: async (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '处理中...' })
      try {
        // 兼容不同平台的输入回传：仅在确实输入了内容时作为取消原因提交
        const typed = (res.content || '').trim()
        const reason = (typed && typed !== modalContent) ? typed : ''
        await adminApi.cancelVolunteer(item.id, reason, userStore.isTeacher)
        uni.hideLoading()
        uni.showToast({ title: '已取消资格', icon: 'success' })
        await loadList()
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: '操作失败：' + (e.message || '请重试'), icon: 'none' })
      }
    }
  })
}

function handleUndoCancel(item) {
  uni.showModal({
    title: '撤销取消',
    content: '确认恢复「' + item.name + '」的志愿者资格吗？',
    confirmText: '确认恢复',
    cancelText: '再想想',
    success: async (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '处理中...' })
      try {
        await adminApi.undoCancelVolunteer(item.id, userStore.isTeacher)
        uni.hideLoading()
        uni.showToast({ title: '已恢复资格', icon: 'success' })
        await loadList()
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: '操作失败：' + (e.message || '请重试'), icon: 'none' })
      }
    }
  })
}

// ========== 问答审核 ==========
async function handleQaAudit(id, status) {
  const action = status === 'APPROVED' ? '通过' : '拒绝'
  uni.showModal({
    title: '确认操作',
    content: `确定要${action}该问答吗？`,
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '处理中...' })
        try {
          await adminApi.auditQA({ id, status })
          uni.hideLoading()
          uni.showToast({ title: `${action}成功`, icon: 'success' })
          await loadList()
        } catch (e) {
          uni.hideLoading()
          uni.showToast({ title: `${action}失败`, icon: 'none' })
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.audit-list {
  min-height: 100vh;
  background: #F2F6FB;
  padding-bottom: 40rpx;

  &__city-bar {
    background: #fff;
    padding: 16rpx 0 8rpx;
    white-space: nowrap;

    &-inner {
      display: inline-flex;
      padding: 0 24rpx;
      gap: 12rpx;
    }
  }

  &__search {
    padding: 16rpx 24rpx;
    background: #fff;

    &-bar {
      display: flex;
      align-items: center;
      background: #F2F6FB;
      border-radius: 999rpx;
      padding: 14rpx 24rpx;
    }

    &-icon {
      font-size: 26rpx;
      margin-right: 12rpx;
      color: #94a3b8;
    }

    &-input {
      flex: 1;
      font-size: 26rpx;
      color: #333;
    }

    &-clear {
      font-size: 32rpx;
      color: #94a3b8;
      padding: 0 8rpx;
    }
  }

  &__tabs {
    display: flex;
    background: #fff;
    padding: 16rpx 24rpx;
    gap: 16rpx;
  }

  &__content {
    padding: 24rpx;
  }
}

.tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 999rpx;
  background: #f5f5f5;

  &--active {
    background: #5B9BD5;

    .tab__text {
      color: #fff;
    }
  }

  &__text {
    font-size: 26rpx;
    color: #666;
  }
}

.city-chip {
  display: inline-flex;
  align-items: center;
  padding: 10rpx 26rpx;
  border-radius: 999rpx;
  background: #f5f5f5;

  &--active {
    background: #5B9BD5;

    .city-chip__text {
      color: #fff;
    }
  }

  &__text {
    font-size: 24rpx;
    color: #666;
  }
}

.audit-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16rpx;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
  }

  &__status {
    font-size: 22rpx;
    padding: 4rpx 16rpx;
    border-radius: 999rpx;

    &--pending {
      color: #faad14;
      background: rgba(250, 173, 20, 0.1);
    }

    &--approved {
      color: #52c41a;
      background: rgba(82, 196, 26, 0.1);
    }

    &--rejected {
      color: #ff4d4f;
      background: rgba(255, 77, 79, 0.1);
    }

    &--cancelled {
      color: #8c8c8c;
      background: rgba(140, 140, 140, 0.1);
    }

    &--replaced {
      color: #8c8c8c;
      background: rgba(140, 140, 140, 0.1);
    }
  }

  // 队伍角色申请标签（申请队长/队长转让）
  &__role-tag {
    font-weight: 500;
  }

  &__role--captain {
    color: #d97706;
    background: rgba(250, 173, 20, 0.14);
  }

  &__role--vice {
    color: #5B9BD5;
    background: rgba(91, 155, 213, 0.14);
  }

  &__role--transfer {
    color: #722ed1;
    background: rgba(114, 46, 209, 0.12);
  }

  &__question {
    margin-top: 8rpx;
    padding: 16rpx;
    background: #f8fafc;
    border-radius: 12rpx;

    &-text {
      font-size: 26rpx;
      color: #555;
      line-height: 40rpx;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 3;
      overflow: hidden;
    }
  }

  &__info {
    margin-bottom: 16rpx;
  }

  &__info-item {
    font-size: 24rpx;
    color: #666;
    display: block;
    margin-bottom: 8rpx;
  }

  &__actions {
    display: flex;
    gap: 16rpx;
    justify-content: flex-end;
    padding-top: 16rpx;
    border-top: 2rpx solid #f5f5f5;
  }
}

.audit-btn {
  padding: 12rpx 32rpx;
  border-radius: 999rpx;

  &--approve {
    background: #5B9BD5;
  }

  &--reject {
    background: #ff4d4f;
  }

  &__text {
    font-size: 24rpx;
    color: #fff;
  }
}

.empty {
  padding: 80rpx;
  text-align: center;

  &__text {
    font-size: 28rpx;
    color: #999;
  }
}

// 高中管理按钮
.audit-card__hs-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  padding-top: 16rpx;
  border-top: 2rpx solid #f0f2f5;
  margin-top: 8rpx;
}

.audit-card__cancel-info {
  flex: 1;

  &-text {
    font-size: 22rpx;
    color: #a0aec0;
  }
}

.hs-btn {
  display: inline-flex;
  align-items: center;
  padding: 10rpx 24rpx;
  background: #f0f7ff;
  border-radius: 999rpx;
  border: 2rpx solid #b8d4fe;

  &--danger {
    background: #fff1f0;
    border-color: #ffa39e;

    .hs-btn__text {
      color: #d4380d;
    }
  }

  &--warn {
    background: #fffbe6;
    border-color: #ffe58f;

    .hs-btn__text {
      color: #d48806;
    }
  }

  &--granted {
    background: #f6ffed;
    border-color: #b7eb8f;

    .hs-btn__text {
      color: #389e0d;
      font-weight: 600;
    }
  }

  &__text {
    font-size: 24rpx;
    color: #4A8BC4;
    font-weight: 500;
  }
}

// 高中分配弹窗
.hs-modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.hs-modal {
  width: 100%;
  max-height: 70vh;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  display: flex;
  flex-direction: column;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 32rpx 32rpx 20rpx;
    border-bottom: 2rpx solid #f0f2f5;
  }

  &__title {
    font-size: 30rpx;
    font-weight: 700;
    color: #2d3748;
  }

  &__close {
    width: 48rpx; height: 48rpx;
    background: #f0f2f5;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;

    &-text { font-size: 26rpx; color: #718096; }
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 24rpx 32rpx;
  }

  &__hint {
    font-size: 24rpx;
    color: #a0aec0;
    display: block;
    margin-bottom: 20rpx;
  }

  &__loading {
    padding: 60rpx 0;
    text-align: center;
    &-text { font-size: 26rpx; color: #a0aec0; }
  }

  &__footer {
    display: flex;
    gap: 16rpx;
    padding: 20rpx 32rpx 40rpx;
    border-top: 2rpx solid #f0f2f5;
  }
}

.hs-modal-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  &--cancel {
    background: #f0f2f5;
    &-text { color: #718096; }
  }

  &--save {
    background: linear-gradient(135deg, #4A8BC4, #3182ce);
    box-shadow: 0 6rpx 18rpx rgba(43, 108, 176, 0.25);
    &-text { color: #fff; }
  }

  &__text {
    font-size: 28rpx;
    font-weight: 600;
  }
}

// 高中复选框列表
.hs-checkbox-list {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.hs-checkbox-item {
  display: flex;
  align-items: center;
  padding: 20rpx 16rpx;
  background: #f7fafc;
  border-radius: 14rpx;
  border: 2rpx solid transparent;

  &--checked {
    background: #ebf8ff;
    border-color: #90cdf4;
  }

  &__name {
    font-size: 28rpx;
    color: #2d3748;
    font-weight: 500;
    flex: 1;
    margin-left: 16rpx;
  }

  &__region {
    font-size: 22rpx;
    color: #a0aec0;
  }
}

.hs-checkbox {
  width: 44rpx; height: 44rpx;
  border-radius: 10rpx;
  border: 2rpx solid #cbd5e0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &--on {
    background: #3182ce;
    border-color: #3182ce;
  }

  &__icon {
    font-size: 26rpx;
    color: #fff;
    font-weight: 700;
  }
}

.hs-empty {
  padding: 60rpx 0;
  text-align: center;

  &__text {
    font-size: 26rpx;
    color: #a0aec0;
  }
}
</style>
