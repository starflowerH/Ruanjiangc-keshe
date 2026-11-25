<template>
  <view class="penalties">
    <NavBar title="处罚与申诉" />
    <view class="penalty" v-for="item in list" :key="item.id">
      <view class="penalty__head">
        <text>第 {{ item.offenseNumber }} 次处罚</text>
        <text :class="['penalty__status', item.status === 'REVOKED' && 'penalty__status--revoked']">{{ statusText(item) }}</text>
      </view>
      <text class="penalty__time">禁言至 {{ item.mutedUntil }}</text>
      <button class="penalty__button" v-if="canAppeal(item)" @tap="appeal(item)">提交申诉</button>
    </view>
    <view class="penalties__empty" v-if="!list.length">暂无处罚记录</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import * as forumApi from '@/api/forum'
const list = ref([])
onShow(async () => { try { list.value = (await forumApi.getMyPenalties()) || [] } catch { list.value = [] } })
// 处罚到期：数据库状态仍为 ACTIVE，前端按禁言截止时间派生显示状态，避免"已失效仍显示生效中"
function isExpired(item) {
  return item.status === 'ACTIVE' && item.mutedUntil && item.mutedUntil <= currentTimeStr()
}
function currentTimeStr() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
const statusText = (item) => item.status === 'REVOKED' ? '已撤销' : (isExpired(item) ? '已失效' : '生效中')
const canAppeal = (item) => item.status !== 'REVOKED' && !isExpired(item)
function appeal(item) {
  uni.showModal({ title: '处罚申诉', editable: true, placeholderText: '请说明申诉理由', success: async (res) => {
    if (!res.confirm || !(res.content || '').trim()) return
    try { await forumApi.submitAppeal({ penaltyId: item.id, reason: res.content.trim() }); uni.showToast({ title: '申诉已提交', icon: 'success' }); await load() }
    catch (e) { uni.showToast({ title: e.message || '提交失败', icon: 'none' }) }
  } })
}
async function load() {
  try { list.value = (await forumApi.getMyPenalties()) || [] } catch { list.value = [] }
}
</script>

<style lang="scss" scoped>
.penalties { min-height: 100vh; background: #f3f6f9; padding-bottom: 40rpx; }
.penalty { margin: 20rpx 24rpx; padding: 24rpx; background: #fff; border-radius: 8rpx; }
.penalty__head { display: flex; justify-content: space-between; font-size: 28rpx; font-weight: 600; }
.penalty__status { color: #b91c1c; }.penalty__status--revoked { color: #15803d; }
.penalty__time { display: block; margin: 16rpx 0; font-size: 24rpx; color: #64748b; }
.penalty__button { margin: 0; width: 180rpx; height: 64rpx; line-height: 64rpx; font-size: 24rpx; color: #1d4ed8; background: #eff6ff; border-radius: 6rpx; }
.penalties__empty { padding: 120rpx 0; text-align: center; color: #94a3b8; }
</style>
