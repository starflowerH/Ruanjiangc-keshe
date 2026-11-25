<template>
  <view class="student-page">
    <view class="safe-area" /><view class="page-top"><view><text class="eyebrow">高校</text><text class="page-title">找到真正走过的人</text></view></view>
    <view class="intro">每一所高校，都能找到真实在读的学长学姐。</view>
    <view class="search"><text>⌕</text><input v-model="keyword" placeholder="搜索大学名称" /></view>
    <scroll-view scroll-x class="filters"><text v-for="item in types" :key="item" class="filter" :class="{active: type===item}" @tap="type=item">{{ item }}</text></scroll-view>
    <view class="section-head"><text>已到访高校</text><text class="count">{{ filtered.length }} 所</text></view>
    <view class="school-list"><view v-for="school in filtered" :key="school.name" class="school-row" @tap="openSchool(school)"><view class="school-logo">{{ school.name.slice(0,1) }}</view><view class="school-main"><text class="school-name">{{ school.name }}</text><text class="school-meta">{{ school.type }} · {{ school.mentors }} 位学长学姐</text><text class="school-note">{{ school.note }}</text></view><text class="arrow">›</text></view><view v-if="!filtered.length" class="empty">没有找到匹配高校</view></view>
    <TabBar currentPath="/pages/student/university-list" />
  </view>
</template>
<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TabBar from '@/components/TabBar.vue'
import * as universityApi from '@/api/university'
const keyword=ref(''); const type=ref('全部'); const types=['全部','综合','理工','财经','医药']; const schools=ref([])
const filtered=computed(()=>schools.value.filter(s=>(type.value==='全部'||s.type===type.value)&&(!keyword.value||s.name.includes(keyword.value))))
onShow(loadSchools)
async function loadSchools(){try{const result=await universityApi.getUniversityList({page:1,size:20});const rows=result?.records||result||[];schools.value=rows.map(item=>({id:item.id,name:item.name||item.universityName||'',type:item.typeName||item.categoryName||item.type||'综合',mentors:item.volunteerCount||item.mentorCount||0,note:item.introduction||item.description||''})).filter(item=>item.id&&item.name)}catch(error){schools.value=[]}}
function openSchool(s){uni.navigateTo({url:`/pages/common/university-detail?id=${s.id}`})}
</script>
<style lang="scss" scoped>
.student-page{min-height:100vh;padding:0 32rpx 128rpx;background:#f3f7fb;color:#1f2937}.safe-area{height:env(safe-area-inset-top);margin:0 -32rpx;background:#fff}.page-top{display:flex;justify-content:space-between;align-items:flex-end;margin:0 -32rpx;padding:30rpx 32rpx 24rpx;background:#fff}.eyebrow{display:block;color:#397cba;font-size:22rpx;font-weight:700}.page-title{display:block;margin-top:8rpx;font-size:44rpx;font-weight:800}.top-action{color:#397cba;font-size:23rpx}.intro{padding:18rpx 0;color:#7b8b99;font-size:24rpx}.search{display:flex;align-items:center;gap:12rpx;padding:0 20rpx;height:84rpx;border-radius:20rpx;background:#eaf3fa}.search text{font-size:34rpx;color:#397cba}.search input{flex:1;font-size:25rpx}.filters{white-space:nowrap;padding:24rpx 0}.filter{display:inline-block;margin-right:12rpx;padding:13rpx 22rpx;border-radius:999rpx;background:#e8f2f9;color:#718394;font-size:23rpx}.filter.active{background:#5b9bd5;color:#fff;font-weight:700}.section-head{display:flex;justify-content:space-between;align-items:center;margin:4rpx 0 16rpx;font-size:31rpx;font-weight:800}.count{color:#397cba;font-size:23rpx;font-weight:500}.school-list{background:#fff;border-radius:20rpx}.school-row{display:flex;align-items:center;gap:18rpx;padding:24rpx;border-bottom:1rpx solid #edf2f6}.school-row:last-child{border-bottom:0}.school-logo{display:grid;place-items:center;flex:0 0 76rpx;height:76rpx;border-radius:18rpx;background:#dcecf8;color:#397cba;font-size:32rpx;font-weight:800}.school-main{flex:1;min-width:0}.school-name,.school-meta,.school-note{display:block}.school-name{font-size:28rpx;font-weight:800}.school-meta{margin-top:5rpx;color:#397cba;font-size:22rpx}.school-note{margin-top:8rpx;color:#8695a2;font-size:21rpx}.arrow{color:#9aaab8;font-size:38rpx}.empty{padding:48rpx;text-align:center;color:#8998a5;font-size:24rpx}
.safe-area{height:calc(env(safe-area-inset-top) + 16rpx);min-height:64rpx}.page-top{justify-content:flex-start}.top-action{display:none}
</style>
