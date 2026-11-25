<template>
  <view class="uni-detail">
    <NavBar title="高校详情" />

    <view class="uni-detail__header">
      <image class="uni-detail__logo" :src="university.logo || '/static/images/default-uni.png'" mode="aspectFill" />
      <view class="uni-detail__header-info">
        <text class="uni-detail__name">{{ university.name }}</text>
        <text class="uni-detail__slogan" v-if="university.slogan">{{ university.slogan }}</text>
        <view class="uni-detail__tags" v-if="university.tags">
          <text class="uni-detail__tag" v-for="tag in university.tags" :key="tag">{{ tag }}</text>
        </view>
      </view>
    </view>

    <view class="uni-detail__tabs">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        :class="['uni-detail__tab', activeTab === tab.key && 'uni-detail__tab--active']"
        @tap="activeTab = tab.key"
      >
        <text class="uni-detail__tab-text">{{ tab.label }}</text>
      </view>
    </view>

    <!-- 学校简介 -->
    <view class="uni-detail__section" v-if="activeTab === 'intro'">
      <view class="uni-detail__card">
        <text class="uni-detail__card-title">学校简介</text>
        <text class="uni-detail__card-content">{{ university.intro || '暂无简介' }}</text>
      </view>
    </view>

    <!-- 专业介绍 -->
    <view class="uni-detail__section" v-if="activeTab === 'major'">
      <view class="uni-detail__card" v-for="major in majors" :key="major.id">
        <text class="uni-detail__major-name">{{ major.name }}</text>
        <text class="uni-detail__major-desc">{{ major.description }}</text>
        <view class="uni-detail__major-tags">
          <text class="uni-detail__major-tag" v-if="major.category">{{ major.category }}</text>
          <text class="uni-detail__major-tag" v-if="major.degree">{{ major.degree }}</text>
        </view>
      </view>
      <Empty v-if="!majors.length" text="暂无专业信息" icon="📖" />
    </view>

    <!-- 分数线 -->
    <view class="uni-detail__section" v-if="activeTab === 'score'">
      <view class="uni-detail__card" v-for="score in scoreLines" :key="score.id">
        <view class="uni-detail__score-head">
          <text class="uni-detail__score-year">{{ score.year }}年</text>
          <text class="uni-detail__score-type">{{ score.localType }}</text>
          <text class="uni-detail__score-batch">{{ score.localBatch }}</text>
        </view>
        <view class="uni-detail__score-row">
          <text class="uni-detail__score-label">最低分</text>
          <text class="uni-detail__score-value">{{ score.minScore ?? '-' }}</text>
        </view>
        <view class="uni-detail__score-row">
          <text class="uni-detail__score-label">最低位次</text>
          <text class="uni-detail__score-value">{{ score.minSection ? score.minSection + ' 名' : '-' }}</text>
        </view>
      </view>
      <Empty v-if="!scoreLines.length" text="暂无分数线信息" icon="📊" />
    </view>

    <!-- 来过本校的学长学姐 -->
    <view class="uni-detail__section" v-if="activeTab === 'volunteer'">
      <StudentCard
        v-for="volunteer in volunteers"
        :key="volunteer.id"
        :data="volunteer"
        :showAsk="isStudent"
        @ask="handleAsk"
      />
      <Empty v-if="!volunteers.length" text="暂无学长学姐信息" icon="👥" />
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import StudentCard from '@/components/StudentCard.vue'
import Empty from '@/components/Empty.vue'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import * as universityApi from '@/api/university'
import * as volunteerApi from '@/api/team'
import { request } from '@/api/index'

const userStore = useUserStore()
const appStore = useAppStore()
const isStudent = computed(() => userStore.isStudent)

const universityId = ref(null)
const university = reactive({
  id: null,
  name: '',
  logo: '',
  slogan: '',
  intro: '',
  tags: []
})
const activeTab = ref('intro')
const majors = ref([])
const scoreLines = ref([])
const volunteers = ref([])
const loading = ref(false)

const tabs = [
  { key: 'intro', label: '简介' },
  { key: 'major', label: '专业' },
  { key: 'score', label: '分数线' },
  { key: 'volunteer', label: '学长学姐' }
]

// 大学简介关键词映射 - 用于生成 slogan 和 tags
const UNI_FEATURES = {
  '清华': { slogan: '自强不息，厚德载物', tags: ['985', '211', '双一流', 'C9'], intro: '清华大学（Tsinghua University）是中国最著名的高等学府之一，成立于1911年。学校以"自强不息，厚德载物"为校训，在工程、计算机、管理等领域具有世界级影响力。清华大学拥有一流的师资力量和研究设施，培养了众多杰出人才。' },
  '北大': { slogan: '思想自由，兼容并包', tags: ['985', '211', '双一流', 'C9'], intro: '北京大学（Peking University）创立于1898年，是中国第一所国立综合性大学。学校以文理医工协调发展著称，在人文社科、自然科学、医学等领域享有盛誉。北大以其深厚的文化底蕴和学术传统闻名于世。' },
  '复旦': { slogan: '博学而笃志，切问而近思', tags: ['985', '211', '双一流', 'C9'], intro: '复旦大学创建于1905年，是中国历史最悠久的高等学府之一。学校在文、理、医、工各领域均衡发展，尤其在医学、经济学、新闻学等方面具有突出优势。' },
  '上海交大': { slogan: '饮水思源，爱国荣校', tags: ['985', '211', '双一流', 'C9'], intro: '上海交通大学始建于1896年，是中国历史最悠久的高等学府之一。学校在工程、医学、管理等领域处于国内领先地位，以其严谨的学风和创新精神著称。' },
  '浙大': { slogan: '求是创新', tags: ['985', '211', '双一流', 'C9'], intro: '浙江大学成立于1897年，是一所综合性全国重点大学。学校学科门类齐全，在工科、农学、医学等领域具有显著优势，被誉为"东方剑桥"。' },
  '南京': { slogan: '诚朴雄伟，励学敦行', tags: ['985', '211', '双一流', 'C9'], intro: '南京大学肇始于1902年，是中国著名的高等学府。学校以文理见长，在天文学、地质学、物理学等领域具有国际影响力。' }
}

// 模拟专业数据（按大学类型生成）
const MOCK_MAJORS_BY_TYPE = {
  '综合': [
    { id: 1, name: '计算机科学与技术', description: '培养具备计算机系统与软件开发和设计能力的高级专门人才', category: '工学', degree: '本科' },
    { id: 2, name: '经济学', description: '研究经济活动规律，培养经济分析和管理能力', category: '经济学', degree: '本科' },
    { id: 3, name: '法学', description: '系统学习法律知识，培养法律实务和研究能力', category: '法学', degree: '本科' },
    { id: 4, name: '工商管理', description: '培养企业管理、市场营销和战略规划能力', category: '管理学', degree: '本科' },
    { id: 5, name: '临床医学', description: '培养具备临床诊疗能力的医学专门人才', category: '医学', degree: '本科' }
  ],
  '理工': [
    { id: 1, name: '计算机科学与技术', description: '培养具备计算机系统与软件开发能力的高级人才', category: '工学', degree: '本科' },
    { id: 2, name: '电子信息工程', description: '研究电子信息系统设计、开发和应用', category: '工学', degree: '本科' },
    { id: 3, name: '机械工程', description: '培养机械设计制造及其自动化方面的专业人才', category: '工学', degree: '本科' },
    { id: 4, name: '土木工程', description: '培养从事土木工程设计和施工管理的高级人才', category: '工学', degree: '本科' },
    { id: 5, name: '数学与应用数学', description: '培养数学理论和应用研究能力', category: '理学', degree: '本科' }
  ],
  '师范': [
    { id: 1, name: '汉语言文学', description: '培养中文教学和文学研究能力', category: '文学', degree: '本科' },
    { id: 2, name: '数学与应用数学', description: '培养数学教学和研究能力', category: '理学', degree: '本科' },
    { id: 3, name: '英语', description: '培养英语教学和翻译能力', category: '文学', degree: '本科' },
    { id: 4, name: '教育学', description: '研究教育理论和实践', category: '教育学', degree: '本科' },
    { id: 5, name: '物理学', description: '培养物理教学和研究能力', category: '理学', degree: '本科' }
  ]
}

onLoad((options) => {
  if (options.id && options.id !== 'undefined') {
    universityId.value = options.id
    loadDetail(options.id)
  }
})

async function loadDetail(id) {
  loading.value = true
  uni.showLoading({ title: '加载中...' })
  try {
    const detail = await universityApi.getUniversityDetail(id)
    // 映射后端字段到前端字段
    mapUniversityData(detail)
    // 加载真实宣传资料（有数据则优先展示）
    await loadContent(id)
    // 加载真实录取分数线（掌上高考数据）
    await loadScoreLines(id)
    // 专业数据兜底（暂用模拟，分数线已改真实）
    generateMockData(detail)
    // 加载对应大学的志愿者
    await loadVolunteers(detail)
  } catch (e) {
    console.error('加载高校详情失败:', e)
  } finally {
    loading.value = false
    uni.hideLoading()
  }
}

// 加载真实录取分数线（GET /api/university/{id}/score-lines）
async function loadScoreLines(id) {
  try {
    const lines = await universityApi.getUniversityScoreLines(id)
    if (Array.isArray(lines)) {
      scoreLines.value = lines
    }
  } catch (e) {
    console.error('加载录取分数线失败:', e)
    scoreLines.value = []
  }
}

function mapUniversityData(detail) {
  university.id = detail.id
  university.name = detail.name || ''
  university.logo = detail.logoUrl || ''
  university.intro = detail.description || ''
  // 根据大学名称匹配特征信息
  const name = detail.name || ''
  const feature = findUniFeature(name)
  university.slogan = feature?.slogan || ''
  university.tags = feature?.tags || []
  // 如果有匹配的详细简介则使用
  if (feature?.intro && !detail.description) {
    university.intro = feature.intro
  }
}

function findUniFeature(name) {
  for (const [key, value] of Object.entries(UNI_FEATURES)) {
    if (name.includes(key)) return value
  }
  // 默认综合类
  return { slogan: '', tags: [] }
}

// 加载真实宣传资料（公开接口，无资料时返回空）
// 后端 UniversityContent 实体：majors/scoreLines 为 JSON 字符串、tags 为逗号分隔字符串
async function loadContent(id) {
  try {
    const content = await request({
      url: `/api/university-content/${id}`,
      method: 'GET',
      silent: true
    })
    if (!content) return
    if (content.logoUrl) university.logo = content.logoUrl
    if (content.slogan) university.slogan = content.slogan
    if (content.description) university.intro = content.description
    if (content.tags) {
      university.tags = String(content.tags).split(',').map(t => t.trim()).filter(Boolean)
    }
    if (content.majors) {
      try {
        const parsed = JSON.parse(content.majors)
        if (Array.isArray(parsed) && parsed.length) {
          // 真实数据无 id 字段，补上自增 id 供 :key 使用
          majors.value = parsed.map((m, i) => ({ ...m, id: m.id !== undefined ? m.id : i }))
        }
      } catch (e) {
        // 兼容逗号分隔的历史数据（如"计算机科学与技术,机械工程"）
        majors.value = String(content.majors).split(',')
          .map((name, i) => ({ id: i, name: name.trim() }))
          .filter(m => m.name)
      }
    }
    if (content.scoreLines) {
      try {
        const parsed = JSON.parse(content.scoreLines)
        if (Array.isArray(parsed) && parsed.length) {
          scoreLines.value = parsed
        }
      } catch (e) {
        console.error('解析分数线数据失败:', e)
      }
    }
  } catch (e) {
    console.error('加载大学宣传资料失败:', e)
  }
}

function getUniType(name) {
  if (name.includes('师范') || name.includes('华东师范') || name.includes('北京师范')) return '师范'
  if (name.includes('科技') || name.includes('理工') || name.includes('工业') || name.includes('交通') || name.includes('航空') || name.includes('邮电')) return '理工'
  return '综合'
}

function generateMockData(detail) {
  const name = detail.name || ''
  const uniType = getUniType(name)
  const baseMajors = MOCK_MAJORS_BY_TYPE[uniType] || MOCK_MAJORS_BY_TYPE['综合']
  // 为每所大学生成不同的专业ID（仅当无真实数据时生成）
  const uniId = detail.id || 1
  if (!majors.value.length) {
    majors.value = baseMajors.map((m, i) => ({
      ...m,
      id: uniId * 100 + i
    }))
  }
  // 分数线不再生成模拟数据：真实分数线由 loadScoreLines 从后端拉取，
  // 无数据时展示「暂无分数线信息」（数据必须真实，不编造）
}

async function loadVolunteers(detail) {
  try {
    // 从 store 获取当前学生的高中 ID
    const highSchoolId = userStore.userInfo?.highSchoolId || appStore.selectedHighSchoolId
    if (highSchoolId) {
      const volunteerList = await volunteerApi.getTeamsByHighSchool(highSchoolId)
      // 过滤出当前大学的志愿者
      const uniName = detail.name || ''
      volunteers.value = (volunteerList || []).filter(v => v.universityName === uniName)
    } else {
      volunteers.value = []
    }
  } catch (e) {
    console.error('加载志愿者列表失败:', e)
    volunteers.value = []
  }
}

// 后端不可用时的模拟数据
function generateMockUniversity(id) {
  const mockUnis = {
    1: { name: '清华大学', slogan: '自强不息，厚德载物', tags: ['985', '211', '双一流', 'C9'], intro: '清华大学是中国最著名的高等学府之一，成立于1911年。学校以"自强不息，厚德载物"为校训，在工程、计算机、管理等领域具有世界级影响力。' },
    2: { name: '北京大学', slogan: '思想自由，兼容并包', tags: ['985', '211', '双一流', 'C9'], intro: '北京大学创立于1898年，是中国第一所国立综合性大学。在人文社科、自然科学、医学等领域享有盛誉。' },
    3: { name: '复旦大学', slogan: '博学而笃志，切问而近思', tags: ['985', '211', '双一流', 'C9'], intro: '复旦大学创建于1905年，是中国历史最悠久的高等学府之一。在医学、经济学、新闻学等方面具有突出优势。' },
    4: { name: '上海交通大学', slogan: '饮水思源，爱国荣校', tags: ['985', '211', '双一流', 'C9'], intro: '上海交通大学始建于1896年，在工程、医学、管理等领域处于国内领先地位。' },
    5: { name: '浙江大学', slogan: '求是创新', tags: ['985', '211', '双一流', 'C9'], intro: '浙江大学成立于1897年，学科门类齐全，在工科、农学、医学等领域具有显著优势。' }
  }
  const mock = mockUnis[id] || { name: '未知大学', slogan: '', tags: [], intro: '暂无学校简介信息' }
  university.id = id
  university.name = mock.name
  university.logo = ''
  university.slogan = mock.slogan
  university.intro = mock.intro
  university.tags = mock.tags
  // 生成模拟专业
  const uniType = getUniType(mock.name)
  const baseMajors = MOCK_MAJORS_BY_TYPE[uniType] || MOCK_MAJORS_BY_TYPE['综合']
  const uniId = id || 1
  majors.value = baseMajors.map((m, i) => ({ ...m, id: uniId * 100 + i }))
  // 生成模拟分数线
  const currentYear = new Date().getFullYear()
  scoreLines.value = []
  for (let y = currentYear - 1; y >= currentYear - 3; y--) {
    scoreLines.value.push({
      year: y,
      scienceMin: 600 + Math.floor(Math.random() * 80),
      artMin: 580 + Math.floor(Math.random() * 60),
      minRank: 5000 + Math.floor(Math.random() * 15000)
    })
  }
  volunteers.value = []
}

function handleAsk(volunteer) {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/common/login' })
    return
  }
  uni.navigateTo({
    url: `/pages/student/ask-question?volunteerId=${volunteer.id}&volunteerName=${encodeURIComponent(volunteer.name)}&universityName=${encodeURIComponent(volunteer.universityName)}`
  })
}
</script>

<style lang="scss" scoped>
.uni-detail {
  min-height: 100vh;
  background: #F2F6FB;

  &__header {
    background: #fff;
    padding: 32rpx;
    display: flex;
    align-items: center;
  }

  &__logo {
    width: 120rpx;
    height: 120rpx;
    border-radius: 16rpx;
    margin-right: 24rpx;
    flex-shrink: 0;
    background: #f0f0f0;
  }

  &__header-info {
    flex: 1;
  }

  &__name {
    font-size: 36rpx;
    font-weight: 700;
    color: #333;
    display: block;
    margin-bottom: 8rpx;
  }

  &__slogan {
    font-size: 24rpx;
    color: #999;
    display: block;
    margin-bottom: 12rpx;
  }

  &__tags {
    display: flex;
    gap: 8rpx;
  }

  &__tag {
    font-size: 20rpx;
    color: #5B9BD5;
    background: rgba(74, 144, 217, 0.1);
    padding: 4rpx 12rpx;
    border-radius: 999rpx;
  }

  &__tabs {
    display: flex;
    background: #fff;
    padding: 0 16rpx;
    border-bottom: 2rpx solid #f0f0f0;
  }

  &__tab {
    flex: 1;
    padding: 24rpx 0;
    text-align: center;
    position: relative;

    &--active {
      .uni-detail__tab-text {
        color: #5B9BD5;
        font-weight: 600;
      }

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 4rpx;
        background: #5B9BD5;
        border-radius: 999rpx;
      }
    }
  }

  &__tab-text {
    font-size: 28rpx;
    color: #666;
  }

  &__section {
    padding: 24rpx;
  }

  &__card {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 16rpx;
  }

  &__card-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    display: block;
    margin-bottom: 16rpx;
  }

  &__card-content {
    font-size: 26rpx;
    color: #666;
    line-height: 1.8;
  }

  &__major-name {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    display: block;
    margin-bottom: 8rpx;
  }

  &__major-desc {
    font-size: 24rpx;
    color: #666;
    display: block;
    margin-bottom: 12rpx;
    line-height: 1.6;
  }

  &__major-tags {
    display: flex;
    gap: 8rpx;
  }

  &__major-tag {
    font-size: 20rpx;
    color: #999;
    background: #f5f5f5;
    padding: 4rpx 12rpx;
    border-radius: 999rpx;
  }

  &__score-head {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-bottom: 16rpx;
  }

  &__score-year {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
  }

  &__score-type {
    font-size: 22rpx;
    color: #5B9BD5;
    background: rgba(91, 155, 213, 0.12);
    padding: 4rpx 14rpx;
    border-radius: 999rpx;
    font-weight: 500;
  }

  &__score-batch {
    font-size: 22rpx;
    color: #999;
  }

  &__score-row {
    display: flex;
    justify-content: space-between;
    padding: 12rpx 0;
    border-bottom: 2rpx solid #f5f5f5;
  }

  &__score-label {
    font-size: 26rpx;
    color: #666;
  }

  &__score-value {
    font-size: 26rpx;
    color: #333;
    font-weight: 500;
  }
}
</style>
