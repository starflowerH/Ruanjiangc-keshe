<template>
  <view class="community">
    <view class="safe-top" />
    <view class="topbar">
      <view class="brand"><view class="brand-mark">招</view><text>招生 · 志愿者</text></view>
    </view>

    <template v-if="page === 'home'">
      <view class="page-head home-head"><view class="grow"><text class="eyebrow">志愿者首页</text><text class="page-title">你好，{{ displayName }}。</text><text class="subhead">所有论坛问题和互动，都在“答疑”里统一处理。</text></view><button class="notice-button" aria-label="消息通知" @tap="openDetail('messages')"><view class="bell-icon" /><view v-if="unreadCount" class="dot" /></button></view>
      <view v-if="isVolunteerApproved" class="section"><view class="status-card"><view class="status-top"><view class="status-icon">✓</view><view class="grow"><text class="card-title">志愿者身份已认证</text><text class="card-copy">{{ profileSubtitle }}</text></view><text class="pill teal">已通过</text></view><view class="status-bottom"><view><text>目标高中</text><b>{{ profileData?.targetHighSchoolName || '未填写' }}</b></view><view><text>所属队伍</text><b>{{ teamName }}</b></view></view></view></view>
      <view class="section"><view class="composer" @tap="openDetail('compose')"><view class="composer-icon">+</view><view class="grow"><b>分享一段大学体验</b><text>把真实的经历留给正在选择的同学</text></view><button class="composer-button">去发布</button></view></view>
      <view class="section"><view class="section-head"><b>我的动态</b><text class="section-link">最新内容</text></view><view class="feed"><view v-for="update in personalUpdates" :key="`${update.source}-${update.id}`" class="question-card" @tap="openDetail('post', update)"><view class="question-meta"><view class="mini-avatar" :class="update.tone">{{ update.user[0] }}</view><text>{{ update.user }}</text><text class="post-time">{{ update.time }}</text></view><b class="question-title">{{ truncate(update.title, 36) }}</b><text class="question-copy">{{ truncate(update.copy, 64) }}</text><view class="question-bottom"><text class="pill" :class="update.source === 'published' ? 'teal' : 'blue'">{{ update.source === 'published' ? '我发布的帖子' : '我收藏的帖子' }}</text><text class="section-link">查看讨论 ›</text></view></view><view v-if="!personalUpdates.length" class="empty">暂时还没有与你相关的帖子。</view></view></view>
    </template>

    <template v-else-if="page === 'qa'">
      <view class="page-head qa-head"><view class="grow"><text class="eyebrow">答疑论坛</text><text class="page-title">从本校到总论坛，<br />认真回答每一个问题。</text><text class="subhead">以提问与回答为中心，不混入个人记录。</text></view><button class="qa-post-button" @tap="openDetail('compose')">发帖</button></view>
      <view class="section filter-section"><scroll-view scroll-x class="chips"><text class="chip" :class="{ active: scope === 'school' }" @tap="scope = 'school'">本校</text><text class="chip" :class="{ active: scope === 'all' }" @tap="scope = 'all'">总论坛</text></scroll-view><scroll-view scroll-x class="chips topic-chips"><text v-for="item in tags" :key="item" class="chip" :class="{ active: tag === item }" @tap="tag = item">{{ item }}</text></scroll-view></view>
      <view class="section"><view class="feed"><view v-for="post in visiblePosts" :key="post.title" class="post" @longpress="showReport(post)"><view class="post-meta"><view class="mini-avatar" :class="post.tone">{{ post.user[0] }}</view><text class="post-who">{{ post.user }}</text><text class="identity-badge">{{ identityLabel(post.identity) }}</text><text>{{ post.school }}</text><text class="post-time">{{ post.time }}</text></view><view class="post-title"><b>{{ truncate(post.title, 36) }}</b></view><view class="post-body"><text class="post-copy">{{ truncate(post.copy, 72) }}</text></view><view class="post-actions"><button class="action-button" @tap.stop="openDetail('post', post)">{{ post.count }} 条讨论</button><button class="action-button" @tap.stop="togglePostFavorite(post)">收藏</button></view></view><view v-if="!visiblePosts.length" class="empty">这个分区暂时没有相关内容。</view></view></view>
    </template>

    <template v-else-if="page === 'team'">
      <view class="page-head"><text class="eyebrow">队伍协作</text><text class="page-title">{{ teamName }}</text><text class="subhead">{{ teamSubtitle || '暂无队伍资料' }}</text></view>
      <view v-if="teamError" class="section"><view class="team-error"><text>{{ teamError }}</text><text class="team-error__retry" @tap="loadTeam()">重新加载</text></view><text class="team-error__hint">若已重新提交志愿者报名，请退出登录后重新进入，以刷新身份信息。</text></view>
      <view v-if="!teamData?.id" class="section"><view class="feed team-entry"><view class="section-head"><b>加入或创建队伍</b></view><text class="detail-copy">创建队伍后分享队伍码，也可以输入已有队伍码加入。</text><view class="team-entry-actions"><button class="primary team-entry-button" @tap="openDetail('team-create')">创建队伍</button><button class="secondary team-entry-button" @tap="openDetail('team-join')">输入队伍码加入</button></view></view></view>
      <view v-if="teamData?.teamCode" class="section"><view class="team-hero"><view class="team-code"><view><text>队伍邀请码</text><b>{{ teamData.teamCode }}</b></view><button class="copy-button" @tap.stop="notice('邀请码已复制')">复制</button></view></view></view>
      <view class="section"><view class="section-head"><b>队伍成员</b><text class="section-link" @tap="openDetail('members')">查看全部 ›</text></view><view class="feed"><view v-for="member in members" :key="member.name" class="member-row" @tap="openDetail('member', member)"><view class="mini-avatar" :class="member.tone">{{ member.name[0] }}</view><view class="grow"><b>{{ member.name }}</b><text>{{ member.major }} · {{ member.grade }}</text></view><text v-if="member.role" class="pill teal">{{ member.role }}</text><text v-else-if="member.me" class="pill blue">我</text></view></view></view>
      <view class="section"><view class="section-head"><b>协作工具</b></view><view class="feed"><view v-if="isTeamCaptain" class="setting-row" @tap="openDetail('contacts')"><b>队长通讯录</b><text>跨校协调 ›</text></view><view class="setting-row" @tap="openDetail('intro')"><b>编辑队伍介绍</b><text>对成员可见 ›</text></view></view></view>
      <view v-if="isTeamCaptain && teamData?.id" class="section"><view class="feed"><view class="setting-row" @tap="openDetail('target-high-school')"><b>目标高中</b><text>{{ teamData?.highSchoolName || '未设置' }} · 队长可修改 ›</text></view><view class="setting-row" @tap="openDetail('captain-contact')"><b>队长通讯方式</b><text>{{ teamData?.captainContactConfigured ? '已填写' : '待填写' }} ›</text></view></view></view>
    </template>

    <template v-else>
      <view class="profile-card"><view class="profile-head"><view class="avatar">{{ displayName[0] }}</view><view class="grow"><b>{{ displayName }}</b><text>{{ profileSubtitle }}</text></view><text class="link" @tap="openDetail('edit')">编辑资料</text></view></view>
      <view class="section"><view v-if="moderatorStatus?.moderator === true && pendingCount > 0" class="moderation"><view><b>你有社区管理权限</b><text>{{ pendingCount }} 条待审核内容，处理后将同步给发帖人。</text></view><button class="moderation-button" @tap="openDetail('moderate')">去处理</button></view></view>
      <view class="section"><view class="section-head"><b>我的内容</b></view><view class="feed"><view v-for="item in personalItems" :key="item.label" class="setting-row" @tap="openPersonalItem(item)"><b>{{ item.label }}</b><text>{{ item.hint }} ›</text></view></view></view>
      <view class="section"><view class="section-head"><b>志愿者设置</b></view><view class="feed"><view v-for="item in settingsItems" :key="item.label" class="setting-row" @tap="openDetail(item.type)"><b>{{ item.label }}</b><text>›</text></view></view></view>
      <view class="section"><button class="logout" @tap="openDetail('logout')">退出登录</button></view>
    </template>

    <view class="bottom-nav"><view class="nav-item" :class="{ active: page === 'home' }" @tap="go('/pages/volunteer/home')"><view class="nav-icon home-icon" /><text>首页</text></view><view class="nav-item" :class="{ active: page === 'qa' }" @tap="go('/pages/volunteer/qa-center')"><view class="nav-icon qa-icon" /><text>答疑</text></view><view class="nav-item" :class="{ active: page === 'team' }" @tap="go('/pages/volunteer/team-view')"><view class="nav-icon team-icon" /><text>队伍</text></view><view class="nav-item" :class="{ active: page === 'profile' }" @tap="go('/pages/volunteer/profile')"><view class="nav-icon profile-icon" /><text>我的</text></view></view>

      <view v-if="detailType" class="mask" @tap="closeDetail"><view class="sheet" @tap.stop=""><view class="sheet-head"><b>{{ detailTitle }}</b><button class="sheet-close-button" @tap="closeDetail">关闭</button></view><view class="sheet-body"><template v-if="detailType === 'post'"><b>{{ detailData.title }}</b><text class="detail-copy">{{ detailData.copy }}</text><view class="detail-list"><view class="detail-row"><b>讨论状态</b><text>{{ detailData.count }} 条讨论</text></view><view class="detail-row"><b>发布范围</b><text>{{ scope === 'school' ? '本校论坛' : '总论坛' }}</text></view></view><button class="primary" @tap="go(`/pages/volunteer/answer-question?id=${detailData.id}`)">回答问题</button></template><template v-else-if="detailType === 'report'"><text class="detail-copy">请说明举报原因。提交后将交由社区审核。</text><view class="report-post"><text>被举报帖子</text><b>{{ truncate(detailData.title || '', 36) }}</b></view><view class="report-reasons"><text v-for="reason in reportReasons" :key="reason" class="report-reason" :class="{ active: reportReason === reason }" @tap="reportReason = reason">{{ reason }}</text></view><textarea v-model="reportDetail" class="report-textarea" placeholder="可补充具体情况（选填）" /><button class="danger" @tap="submitReport">提交举报</button></template><template v-else-if="detailType === 'moderate'"><view class="detail-list"><view class="detail-row"><b>待审核内容</b><text>{{ pendingCount }} 条</text></view><view class="detail-row"><b>审核权限</b><text>{{ moderatorStatus?.moderator ? '已开通' : '未开通' }}</text></view></view><view v-if="pendingPosts.length" class="pending-post" v-for="post in pendingPosts" :key="post.id"><text class="pending-post__title">{{ post.title || '未命名帖子' }}</text><text class="pending-post__copy">{{ post.questionContent || '' }}</text><view class="pending-post__actions"><button class="secondary pending-post__btn" @tap="reviewPending(post, 'REJECTED')">驳回</button><button class="primary pending-post__btn pending-post__approve" @tap="reviewPending(post, 'APPROVED')">通过</button></view></view><text v-else class="detail-copy pending-post__empty">暂无待审核内容</text></template><template v-else-if="detailType === 'progress'"><text class="detail-copy">本周目标是整理专业分享材料，并确认线上答疑安排。</text><view class="task-stats"><view><b>4 / 6</b><text>任务完成</text></view><view><b>12</b><text>已整理问题</text></view><view><b>周日</b><text>截止时间</text></view></view><view class="detail-list"><view class="detail-row"><b>确认线上答疑时间</b><text>已完成</text></view><view class="detail-row"><b>整理计算机专业分享材料</b><text>进行中</text></view><view class="detail-row"><b>发布本周队伍动态</b><text>待开始</text></view></view></template><template v-else-if="detailType === 'compose'"><text class="detail-copy">{{ detailDescription }}</text><view class="compose-identity"><text class="identity-label">发帖身份</text><text class="identity-badge">{{ activeIdentityLabel }}</text></view><input v-model="composeTitle" class="compose-title-input" placeholder="请输入帖子标题" /><textarea v-model="composeContent" :placeholder="detailPlaceholder" /><button class="primary" @tap="saved">提交</button></template><template v-else-if="detailType === 'intro' || detailType === 'help' || detailType === 'appeal'"><text class="detail-copy">{{ detailDescription }}</text><textarea v-model="detailContent" :placeholder="detailPlaceholder" /><button class="primary" @tap="saved">提交</button></template><template v-else-if="detailType === 'logout'"><text class="detail-copy">退出后将返回角色选择页，当前登录状态会被清除。</text><button class="danger" @tap="notice('已退出登录')">确认退出登录</button></template><template v-else><text class="detail-copy">{{ detailDescription }}</text><view class="detail-list"><view v-for="row in detailRows" :key="row[0]" class="detail-row"><b>{{ row[0] }}</b><input v-if="detailType === 'edit'" v-model="row[1]" class="detail-value-input" /><text v-else>{{ row[1] }}</text></view></view><button v-if="detailType === 'edit' || detailType === 'application'" class="primary" @tap="saved">保存</button></template></view></view></view>
    <view v-if="detailType === 'saved' || detailType === 'comments'" class="linked-list-mask" @tap="closeDetail">
      <view class="linked-list-sheet" @tap.stop="">
        <view class="sheet-head"><b>{{ detailTitle }}</b><button class="sheet-close-button" @tap="closeDetail">关闭</button></view>
        <view v-if="linkedItems.length" class="linked-list">
          <view v-for="item in linkedItems" :key="`${item.postId}-${item.id}`" class="linked-list-row" @tap="openLinkedPost(item)"><b>{{ item.title }}</b><text>{{ item.subtitle }}</text><text class="linked-list-arrow">›</text></view>
        </view>
        <view v-else class="empty">暂无内容</view>
      </view>
    </view>
    <view v-if="detailType === 'discussion'" class="discussion-page">
      <view class="discussion-safe-top" />
      <view class="discussion-head"><button class="discussion-back" @tap="closeDetail">返回</button><b>讨论详情</b><view class="discussion-spacer" /></view>
      <scroll-view scroll-y class="discussion-content">
        <view class="discussion-post"><view class="discussion-author-line"><text class="discussion-author">{{ detailData.user }} · {{ detailData.school }}</text><text class="identity-badge">{{ identityLabel(detailData.identity) }}</text></view><b class="discussion-title">{{ detailData.title }}</b><text class="discussion-copy">{{ detailData.copy }}</text></view>
        <view class="discussion-section-head"><text class="comment-count">{{ postComments.length }} 条评论</text></view>
        <view v-if="postComments.length" class="discussion-comments">
          <view v-for="comment in topLevelComments" :key="comment.id" class="comment-thread">
            <view class="comment-row" @longpress="openCommentReport(comment)">
              <view class="comment-avatar">{{ commentInitial(comment) }}</view>
              <view class="comment-main"><view class="comment-author-line"><text class="comment-author">{{ commentName(comment) }}</text><text class="identity-badge">{{ identityLabel(comment) }}</text></view><text class="comment-copy">{{ comment.content }}</text><view class="comment-foot"><text class="comment-meta">{{ comment.createdAt || '刚刚' }}</text><text class="comment-action reply-action" @tap="replyTo(comment)">回复</text><text class="like-action" :class="{ liked: comment.liked }" @tap="likeComment(comment)">♡ {{ comment.likeCount || 0 }}</text></view></view>
            </view>
            <view v-if="repliesOf(comment).length" class="reply-summary" @tap="toggleReplies(comment.id)"><view class="reply-line" /><text>{{ expandedReplies[comment.id] ? '收起' : '展开' }} {{ repliesOf(comment).length }} 条回复</text><text class="reply-chevron">⌄</text></view>
            <view v-if="expandedReplies[comment.id]" class="nested-comments"><view v-for="reply in repliesOf(comment)" :key="reply.id" class="comment-row nested-comment" @longpress="openCommentReport(reply)"><view class="comment-avatar reply-avatar">{{ commentInitial(reply) }}</view><view class="comment-main"><view class="comment-author-line"><text class="comment-author">{{ commentName(reply) }}</text><text class="identity-badge">{{ identityLabel(reply) }}</text></view><text class="comment-copy"><text class="reply-prefix">回复 @{{ commentName(comment) }}：</text>{{ reply.content }}</text><view class="comment-foot"><text class="comment-meta">{{ reply.createdAt || '刚刚' }}</text><text class="comment-action reply-action" @tap="replyTo(reply)">回复</text><text class="like-action" :class="{ liked: reply.liked }" @tap="likeComment(reply)">♡ {{ reply.likeCount || 0 }}</text></view></view></view></view>
          </view>
        </view>
        <view v-else class="discussion-empty">暂时还没有评论，来发表第一条回复。</view>
      </scroll-view>
      <view class="reply-bar"><view v-if="replyTarget" class="reply-target"><text>回复 @{{ commentName(replyTarget) }}</text><text class="reply-cancel" @tap="replyTarget = null">取消</text></view><view class="reply-entry"><view class="identity-picker"><text class="identity-label">本次发言身份</text><text class="identity-badge">{{ activeIdentityLabel }}</text></view><input v-model="replyContent" class="reply-input" :placeholder="replyTarget ? '写下你的回复' : '友善交流，留下你的想法'" /><button class="reply-button" @tap="submitReply">发送</button></view></view>
      <view v-if="commentReportTarget" class="comment-report-mask" @tap="commentReportTarget = null"><view class="comment-report-sheet" @tap.stop=""><view class="sheet-head"><b>举报评论</b><button class="sheet-close-button" @tap="commentReportTarget = null">关闭</button></view><text class="detail-copy">请选择举报原因，提交后将交由社区审核。</text><view class="report-reasons"><text v-for="reason in reportReasons" :key="reason" class="report-reason" :class="{ active: reportReason === reason }" @tap="reportReason = reason">{{ reason }}</text></view><textarea v-model="reportDetail" class="report-textarea" placeholder="可补充具体情况（选填）" /><button class="danger" @tap="submitCommentReport">提交举报</button></view></view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getForumPosts, getMyFavorites, getMyPosts, getQADetail, submitQuestion, toggleFavorite } from '@/api/qa'
import { addComment, getComments, getMyComments, toggleCommentLike } from '@/api/comment'
import { getModeratorStatus, getMyPenalties, getPendingPosts, reportPost, reviewPost, submitAppeal } from '@/api/forum'
import { getMyProfile, updateProfile } from '@/api/profile'
import { getMyEnrollment } from '@/api/enrollment'
import { createTeam, getMyTeam, getTeamMembers, getMemberDetail, joinTeam, updateTeamDescription, updateTeamTarget } from '@/api/team'
import { searchSchools } from '@/api/highschool'
import { getNotifications, getUnreadCount, markAllAsRead } from '@/api/notification'
import { useUserStore } from '@/store/modules/user'

const props = defineProps({ page: { type: String, required: true } })
const userStore = useUserStore()
const scope = ref('school')
const tag = ref('全部')
const answerTab = ref('done')
const reportReason = ref('不当言论')
const reportDetail = ref('')
const postComments = ref([])
const replyContent = ref('')
const composeTitle = ref('')
const composeContent = ref('')
const teamNameInput = ref('')
const teamCodeInput = ref('')
const detailContent = ref('')
const detailRowsData = ref([])
const linkedItems = ref([])
const unreadCount = ref(0)
const favoriteCount = ref(0)
const commentCount = ref(0)
const replyTarget = ref(null)
const commentReportTarget = ref(null)
const expandedReplies = ref({})const activeIdentity = computed(() => isVolunteerApproved.value ? 'volunteer' : 'university')const identityOptions = [{ key: 'university', label: '大学生' }, { key: 'volunteer', label: '志愿者' }, { key: 'student', label: '高中生' }]const activeIdentityLabel = computed(() => identityLabel(activeIdentity.value))
let teamLoadRequest = 0
const profileData = ref(null)
const teamData = ref(null)
const teamError = ref('')
const moderatorStatus = ref(null)
const pendingPosts = ref([])
const pendingCount = ref(0)
const loading = ref(false)
const detailType = ref('')
const detailData = ref({})
const tags = ['全部', '推荐', '专业选择', '校园生活', '志愿填报']
const reportReasons = ['不当言论', '虚假信息', '广告骚扰', '其他']
const posts = ref([])
const members = ref([])
const personalUpdates = ref([])
const personalItems = computed(() => {
  const approved = isVolunteerApproved.value
  const status = profileData.value?.volunteerStatus
  const enrollmentHint = status === 'PENDING' ? '审核中' : status === 'REJECTED' ? '可重新提交' : '填写报名资料'
  return [
    { label: '我的收藏', hint: `${favoriteCount.value} 条`, type: 'saved' },
    { label: '我的评论', hint: `${commentCount.value} 条`, type: 'comments' },
    { label: '申诉中心', hint: '违规记录与申诉', type: 'appeal' },
    { label: '消息通知', hint: unreadCount.value ? `${unreadCount.value} 条未读` : '暂无未读', type: 'messages' },
    { label: approved ? '报名与身份信息' : '志愿者报名', hint: approved ? '已通过审核' : enrollmentHint, type: approved ? 'application' : 'enrollment' }
  ]
})
const settingsItems = [{ label: '帮助与反馈', type: 'help' }]
const displayName = computed(() => profileData.value?.nickname || profileData.value?.name || '未设置昵称')
const profileSubtitle = computed(() => [profileData.value?.universityName, profileData.value?.major].filter(Boolean).join(' · ') || '未完善资料')
const teamName = computed(() => teamData.value?.teamName || '暂未加入队伍')
const teamSubtitle = computed(() => [teamData.value?.highSchoolName ? `目标高中：${teamData.value.highSchoolName}` : '', teamData.value?.memberCount != null ? `${teamData.value.memberCount} 名志愿者` : ''].filter(Boolean).join(' · '))
const isVolunteerApproved = computed(() => ['APPROVED'].includes(profileData.value?.volunteerStatus || profileData.value?.status))
const isTeamCaptain = computed(() => Boolean(userStore.isCaptain || (teamData.value?.captainVolunteerId && teamData.value.captainVolunteerId === userStore.userInfo?.relatedId)))
const visiblePosts = computed(() => posts.value.filter(post => (scope.value === 'all' || post.scope === 'school') && (tag.value === '全部' || (tag.value === '推荐' ? post.recommended : post.topic === tag.value))))
const topLevelComments = computed(() => postComments.value.filter(comment => !comment.parentId))
const detailTitle = computed(() => ({ compose: '发布一条经验', post: '问题详情', report: '举报帖子', progress: '本周宣传准备', members: '队伍成员', member: '成员资料', contacts: '队长通讯录', intro: '编辑队伍介绍', 'captain-contact': '队长通讯方式', edit: '编辑资料', moderate: '社区审核', saved: '我的收藏', comments: '我的评论', appeal: '申诉中心', messages: '消息通知', application: '报名与身份信息', help: '帮助与反馈', answer: '我的回答', logout: '退出登录' })[detailType.value] || '详情')
const detailDescription = computed(() => ({ compose: '分享给正在选择大学的同学。', intro: '仅队伍成员可见，可随时更新介绍。', 'captain-contact': '至少填写一种，供匹配队伍的队长联系。', help: '请描述你遇到的问题，我们会在消息通知中回复。', appeal: '如内容被误判、隐藏或限制展示，可在这里提交复核。', contacts: '用于与同一目标高中的其他队伍协调宣讲时间与资料。', members: '队伍成员资料来自当前队伍。', member: `${detailData.value.name || '该成员'}的队伍资料。`, edit: '可在此更新个人资料。', moderate: '审核状态来自社区审核接口。', saved: '你收藏的问答和经验会集中显示在这里。', comments: '这里记录你在本校和总论坛留下的回复。', messages: '问答回复和队伍动态都会汇总到这里。', application: '志愿者身份已通过审核。', answer: '你已回答的问题会沉淀在这里。' })[detailType.value] || '')
const detailPlaceholder = computed(() => detailType.value === 'compose' ? '例如：大一第一次进实验室时，我做了哪些准备……' : detailType.value === 'help' ? '请描述你遇到的问题' : '请输入内容')
const detailRows = computed(() => detailRowsData.value)
const go = url => uni.reLaunch({ url })
const notice = title => {
  if (title === '已退出登录') {
    userStore.logout()
    uni.reLaunch({ url: '/pages/index/index' })
    return
  }
  uni.showToast({ title, icon: 'none' })
}
const truncate = (text, limit) => text.length > limit ? `${text.slice(0, limit)}...` : text
const showReport = post => { detailData.value = post; detailType.value = 'report' }
async function submitTeamEntry(type, value) {
  const name = typeof value === 'string' ? value.trim() : value?.name?.trim()
  if (!name) return notice(type === 'team-create' ? '请输入队伍名称' : '请输入队伍码')
  try {
    if (type === 'team-create') {
      const contact = value.contact?.trim()
      if (!contact) return notice('请填写至少一种通讯方式')
      const isPhone = /^1\d{10}$/.test(contact)
      await createTeam({ teamName: name, captainContactPhone: isPhone ? contact : '', captainContactWechat: isPhone ? '' : contact })
    } else await joinTeam({ teamCode: name.toUpperCase() })
    await loadTeam()
    notice(type === 'team-create' ? '队伍创建成功' : '已加入队伍')
  } catch (error) {
    notice(error?.message || (type === 'team-create' ? '创建失败，请稍后重试' : '加入失败，请检查队伍码'))
  }
}
async function editTeamTarget() {
  if (!isTeamCaptain.value || !teamData.value?.id) return notice('仅队长可以修改目标高中')
  uni.showModal({ title: '修改目标高中', editable: true, placeholderText: '输入高中名称搜索', success: async result => {
    if (!result.confirm || !result.content?.trim()) return
    try {
      const response = await searchSchools(result.content.trim())
      const schools = Array.isArray(response) ? response : (response?.records || [])
      if (schools.length !== 1) return notice(schools.length ? '搜索到多个结果，请输入更完整的名称' : '没有找到这所高中')
      await updateTeamTarget({ teamId: teamData.value.id, highSchoolId: schools[0].id })
      await loadTeam()
      notice('目标高中已更新')
    } catch (error) { notice(error?.message || '目标高中更新失败') }
  } })
}
const openDetail = async (type, data = {}) => {
  if (type === 'team-create' || type === 'team-join') {
    uni.showModal({
      title: type === 'team-create' ? '创建队伍' : '输入队伍码加入',
      editable: true,
      placeholderText: type === 'team-create' ? '请输入队伍名称' : '请输入队伍码',
      success: result => {
        if (!result.confirm || !result.content?.trim()) return
        if (type === 'team-create') {
          uni.showModal({
            title: '队长通讯方式',
            editable: true,
            placeholderText: '手机号或微信号（至少填写一种）',
            success: contact => {
              if (contact.confirm) submitTeamEntry(type, { name: result.content, contact: contact.content })
            }
          })
        } else submitTeamEntry(type, result.content)
      }
    })
    return
  }
  if (type === 'target-high-school') return editTeamTarget()
  if (type === 'contacts') return go('/pages/volunteer/captain-contacts')
  // 编辑资料跳转完整资料页（含大学修改，弹层内只有昵称/专业/年级）
  if (type === 'edit') return uni.navigateTo({ url: '/pages/common/edit-profile' })
  detailType.value = type === 'post' ? 'discussion' : type
  detailData.value = data
  detailRowsData.value = []
  linkedItems.value = []
  detailContent.value = ''
  if (type === 'compose') { composeTitle.value = ''; composeContent.value = '' }
  if (type === 'post' && data.id) {
    try {
      const comments = await getComments(data.id)
      const list = Array.isArray(comments) ? comments : (comments?.records || [])
      postComments.value = list.flatMap(item => [item, ...(item.children || [])])
    } catch (error) {
      postComments.value = []
    }
  } else if (type === 'messages') {
    // 消息通知包含全部类型（互动 + 系统处罚通知），与学生端通知页保持一致
    const result = await getNotifications({ page: 1, size: 20 })
    detailRowsData.value = recordsOf(result).map(item => [item.content || item.title || '收到新的互动', item.createdAt || '刚刚'])
    await markAllAsRead()
    unreadCount.value = await getUnreadCount()
  } else if (type === 'saved') {
    const result = await getMyFavorites({ page: 1, size: 20 })
    favoriteCount.value = Number(result?.total || 0)
    linkedItems.value = recordsOf(result).map(item => ({ id: item.id, postId: item.id || item.qaId, title: item.title || item.questionContent || '收藏内容', subtitle: item.universityName || item.askedAt || '' }))
    detailRowsData.value = recordsOf(result).map(item => [item.title || item.questionContent || '收藏内容', item.universityName || item.askedAt || ''])
  } else if (type === 'comments') {
    const result = await getMyComments({ page: 1, size: 20 })
    commentCount.value = Number(result?.total || 0)
    linkedItems.value = recordsOf(result).map(item => ({ id: item.id, postId: item.postId, title: item.postTitle || '帖子已删除', subtitle: item.content || item.createdAt || '' }))
    detailRowsData.value = recordsOf(result).map(item => [item.content || '我的评论', item.postTitle || item.createdAt || ''])
  } else if (type === 'appeal') {
    const result = await getMyPenalties()
    const penalties = recordsOf(result)
    detailData.value = penalties[0] || {}
    detailRowsData.value = penalties.map(item => [item.reason || '违规记录', item.status || item.createdAt || '待处理'])
  } else if (type === 'moderate') {
    await loadModeration()
    detailRowsData.value = [['待审核内容', `${pendingCount.value} 条`], ['审核权限', moderatorStatus.value?.moderator ? '已开通' : '未开通']]
  } else if (type === 'members' && members.value.length) {
    detailRowsData.value = members.value.map(item => [item.name, `${item.major || ''} · ${item.grade || ''}`])
  } else if (type === 'member' && data.id) {
    const result = await getMemberDetail(data.id)
    detailRowsData.value = [['专业', result?.major || '未填写'], ['目标高中', result?.targetHighSchoolName || '未填写'], ['可联系时间', result?.availableTime || '未填写']]
  } else if (type === 'edit') {
    detailRowsData.value = [['昵称', profileData.value?.nickname || profileData.value?.name || ''], ['专业', profileData.value?.major || ''], ['年级', profileData.value?.grade || '']]
  } else if (type === 'application') {
    detailRowsData.value = [['志愿者状态', '已通过审核'], ['所属高校', profileData.value?.universityName || '已登记高校'], ['专业与年级', `${profileData.value?.major || '未填写'} · ${profileData.value?.grade || '未填写'}`]]
  }
}
async function openLinkedPost(item) {
  if (!item?.postId) return notice('该内容已不可查看')
  try {
    const post = await getQADetail(item.postId)
    await openDetail('post', toPost(post))
  } catch (error) {
    notice('帖子暂时不可查看')
  }
}
async function submitReply() {
  if (!replyContent.value.trim()) return notice('请输入回复内容')
  try {
    await addComment({ postId: detailData.value.id, content: replyContent.value.trim(), parentId: replyTarget.value?.id || null })
    replyContent.value = ''
    replyTarget.value = null
    await openDetail('post', detailData.value)
  } catch (error) {
    notice('回复失败，请稍后重试')
  }
}
const replyTo = comment => { replyTarget.value = comment }
const identityLabel = value => {  const key = typeof value === 'string' ? value : (value?.identity || value?.userType || value?.authorType)  return identityOptions.find(item => item.key === key)?.label || (key === 'high_school_student' ? '高中生' : '志愿者')}const commentName = comment => comment.userName || comment.nickname || '校园用户'
const commentInitial = comment => commentName(comment)[0]
const repliesOf = comment => postComments.value.filter(item => String(item.parentId || '') === String(comment.id))
const toggleReplies = id => { expandedReplies.value[id] = !expandedReplies.value[id] }
const likeComment = async comment => {
  try {
    const result = await toggleCommentLike(comment.id)
    comment.liked = result?.liked ?? !comment.liked
    comment.likeCount = result?.likeCount ?? Math.max(0, (comment.likeCount || 0) + (comment.liked ? 1 : -1))
  } catch (error) { notice('点赞失败，请稍后重试') }
}
const openCommentReport = comment => { commentReportTarget.value = comment; reportDetail.value = '' }
const submitCommentReport = async () => {
  if (!commentReportTarget.value || !detailData.value.id) return
  try {
    await reportPost({ postId: detailData.value.id, reason: reportReason.value, detail: `评论ID：${commentReportTarget.value.id}\n${commentReportTarget.value.content || ''}\n${reportDetail.value}` })
    commentReportTarget.value = null
    reportDetail.value = ''
    notice('举报已提交')
  } catch (error) { notice('举报失败，请稍后重试') }
}
const closeDetail = () => { detailType.value = '' }
const openPersonalItem = item => {
  if (item.type === 'enrollment') return go('/pages/volunteer/register')
  openDetail(item.type)
}
const saved = async () => {
  if (detailType.value === 'compose') {
    if (!composeTitle.value.trim() || !composeContent.value.trim()) return notice('请填写标题和内容')
    try {
      await submitQuestion({
        title: composeTitle.value.trim(),
        questionContent: composeContent.value.trim(),
        category: 'other',
        targetVolunteerId: null
      })
      closeDetail()
      notice('帖子已发布')
      if (props.page === 'qa') await loadForum()
      return
    } catch (error) {
      notice('发布失败，请稍后重试')
      return
    }
  }
  if (detailType.value === 'edit') {
    await updateProfile({ nickname: detailRowsData.value[0]?.[1], major: detailRowsData.value[1]?.[1], grade: detailRowsData.value[2]?.[1] })
  } else if (detailType.value === 'intro') {
    if (!teamData.value?.id) return notice('暂无队伍')
    await updateTeamDescription({ teamId: teamData.value.id, description: detailContent.value.trim() })
  } else if (detailType.value === 'appeal') {
    if (!detailData.value?.id) return notice('暂无可申诉的处罚记录')
    if (!detailContent.value.trim()) return notice('请填写申诉理由')
    await submitAppeal({ penaltyId: detailData.value?.id, reason: detailContent.value.trim() })
  }
  closeDetail()
  notice('已保存')
}
const categoryToBackend = { '专业选择': 'major', '校园生活': 'campus', '志愿填报': 'admission' }
const categoryToLabel = { major: '专业选择', campus: '校园生活', admission: '志愿填报', study: '校园生活', other: '全部' }
const toPost = item => ({
  id: item.id,
  scope: item.universityId === profileData.value?.universityId ? 'school' : 'all',
  topic: categoryToLabel[item.category] || '全部',
  recommended: item.isFeatured === 1,
  user: item.askerName || '匿名同学',
  school: item.askerHighSchool || item.universityName || '总论坛',
  time: item.activityAt || item.askedAt || '刚刚',
  activityAt: item.activityAt || item.askedAt,
  title: item.title || item.questionContent,
  copy: item.questionContent,
  count: item.commentCount || 0,
  tone: item.authorType === 'volunteer' ? 'teal' : '',  identity: item.authorType || item.askerType || 'student'
})
const recordsOf = page => Array.isArray(page) ? page : (page?.records || [])

async function loadProfile() {
  profileData.value = await getMyProfile()
  if (!profileData.value?.volunteerStatus) {
    try {
      const data = await getMyEnrollment()
      const list = Array.isArray(data) ? data : (data?.records || [])
      profileData.value = { ...profileData.value, volunteerStatus: list[0]?.status || 'NONE' }
    } catch (error) {
      profileData.value = { ...profileData.value, volunteerStatus: 'NONE' }
    }
  }
}

async function loadForum() {
  const params = { page: 1, size: 20 }
  if (scope.value === 'school' && profileData.value?.universityId) params.universityId = profileData.value.universityId
  if (tag.value === '推荐') params.featured = 1
  else if (tag.value !== '全部') params.category = categoryToBackend[tag.value]
  const result = await getForumPosts(params)
  posts.value = recordsOf(result).map(toPost)
}

async function loadPersonalUpdates() {
  const [myPosts, favorites] = await Promise.all([
    getMyPosts({ page: 1, size: 5 }),
    getMyFavorites({ page: 1, size: 5 })
  ])
  personalUpdates.value = [
    ...recordsOf(myPosts).map(item => ({ ...toPost(item), source: 'published' })),
    ...recordsOf(favorites).map(item => ({ ...toPost(item), source: 'favorite' }))
  ].sort((left, right) => new Date(right.activityAt || 0) - new Date(left.activityAt || 0)).slice(0, 8)
}

async function loadTeam() {
  const requestId = ++teamLoadRequest
  try {
    const team = await getMyTeam()
    if (requestId !== teamLoadRequest) return
    teamData.value = team
    teamError.value = ''
    if (team?.id) {
      const result = await getTeamMembers(team.id)
      if (requestId !== teamLoadRequest) return
      members.value = recordsOf(result).map(item => ({ ...item, name: item.name || item.nickname || '队员', major: item.major || '', grade: item.grade || '', role: item.role === 'CAPTAIN' ? '队长' : '', me: item.me || item.isMe }))
    } else {
      members.value = []
    }
  } catch (error) {
    // 队伍接口失败（如报名未通过审核/旧 token 携带被替换的志愿者ID）时,
    // 把真实原因展示出来，避免"看不到队伍"又不知道为什么。
    if (requestId !== teamLoadRequest) return
    teamData.value = null
    members.value = []
    teamError.value = error?.message || '队伍信息加载失败'
  }
}

async function loadModeration() {
  try {
    const status = await getModeratorStatus()
    moderatorStatus.value = status
    if (status?.moderator) {
      const result = await getPendingPosts({ page: 1, size: 20 })
      pendingPosts.value = recordsOf(result)
      pendingCount.value = pendingPosts.value.length
    } else {
      pendingPosts.value = []
      pendingCount.value = 0
    }
  } catch (error) {
    moderatorStatus.value = null
    pendingPosts.value = []
    pendingCount.value = 0
  }
}

async function reviewPending(post, action) {
  try {
    await reviewPost(post.id, action)
    pendingPosts.value = pendingPosts.value.filter(item => item.id !== post.id)
    pendingCount.value = pendingPosts.value.length
    notice('审核完成')
  } catch (error) { notice(error?.message || '审核失败') }
}

async function loadPageData() {
  loading.value = true
  try {
    await loadProfile()
    unreadCount.value = await getUnreadCount()
    await loadPersonalCounts()
    if (props.page === 'qa') await loadForum()
    if (props.page === 'home') await loadPersonalUpdates()
    if (props.page === 'team' || props.page === 'home') await loadTeam()
    if (props.page === 'profile') await loadModeration()
  } catch (error) {
    // Keep the prototype's local examples available when the user is not logged in or the API is unavailable.
  } finally {
    loading.value = false
  }
}

async function loadPersonalCounts() {
  const [favorites, comments] = await Promise.allSettled([
    getMyFavorites({ page: 1, size: 1 }),
    getMyComments({ page: 1, size: 1 })
  ])
  favoriteCount.value = favorites.status === 'fulfilled' ? Number(favorites.value?.total || 0) : 0
  commentCount.value = comments.status === 'fulfilled' ? Number(comments.value?.total || 0) : 0
}

async function togglePostFavorite(post) {
  if (!post.id) return notice('演示帖子暂不支持收藏')
  try {
    await toggleFavorite(post.id)
    notice('已更新收藏')
  } catch (error) {}
}

async function submitReport() {
  if (!detailData.value.id) return notice('演示帖子暂不支持举报')
  try {
    await reportPost({ postId: detailData.value.id, reason: reportReason.value, detail: reportDetail.value })
    reportDetail.value = ''
    closeDetail()
    notice('举报已提交')
  } catch (error) {}
}

onMounted(loadPageData)
watch(() => props.page, loadPageData)
watch([scope, tag], () => { if (props.page === 'qa' && !loading.value) loadForum().catch(() => {}) })
</script>

<style scoped>
.community{min-height:100vh;padding-bottom:calc(148rpx + env(safe-area-inset-bottom));background:#f4f7fb;color:#17243a;font-size:26rpx}.topbar{height:104rpx;padding:0 32rpx;display:flex;align-items:center;justify-content:space-between;background:#fff;border-bottom:1rpx solid #e5eaf0}.brand,.status-top,.composer,.hero-top,.profile-head,.question-meta,.post-meta,.post-actions,.member-row,.setting-row,.moderation,.sheet-head,.detail-row{display:flex;align-items:center}.brand{gap:16rpx;font-size:30rpx;font-weight:800}.brand-mark{display:grid;place-items:center;width:56rpx;height:56rpx;border-radius:8rpx;background:#2879e8;color:#fff}.notice-button{position:relative;margin:0;padding:12rpx 14rpx;border-radius:8rpx;color:#52657e;background:#f2f6fb;font-size:22rpx}.dot{position:absolute;top:8rpx;right:8rpx;width:10rpx;height:10rpx;border-radius:50%;background:#e4515b}.page-head,.profile-card{padding:38rpx 32rpx 32rpx;background:#fff}.eyebrow{margin-bottom:8rpx;color:#2879e8;font-size:22rpx;font-weight:700}.page-title{display:block;font-size:44rpx;line-height:1.35;font-weight:800}.subhead{margin-top:12rpx;color:#718096;font-size:24rpx;line-height:1.55}.section{padding:32rpx 32rpx 0}.section-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:18rpx;font-size:30rpx}.section-head .section-link,.link{color:#2879e8;font-size:23rpx;font-weight:700}.status-card,.question-card,.feed,.team-hero,.moderation{border:1rpx solid #e5eaf0;border-radius:8rpx;background:#fff}.status-card{padding:28rpx;border-color:#d9eafe;background:#f4f9ff}.status-icon,.composer-icon{display:grid;place-items:center;width:62rpx;height:62rpx;margin-right:18rpx;border-radius:8rpx;color:#2879e8;background:#fff;font-size:32rpx;font-weight:800}.grow{flex:1;min-width:0}.card-title{display:block;font-size:29rpx;font-weight:800}.card-copy{margin-top:5rpx;color:#60728a;font-size:23rpx}.pill{display:inline-flex;padding:6rpx 10rpx;border-radius:5rpx;font-size:21rpx;font-weight:700;white-space:nowrap}.teal{color:#087a6d;background:#e6f7f4}.blue{color:#2879e8;background:#eaf3ff}.orange{color:#a96800;background:#fff4db}.status-bottom{display:flex;gap:24rpx;margin-top:24rpx;padding-top:20rpx;border-top:1rpx solid #d8e8f7}.composer{gap:16rpx;min-height:116rpx;padding:22rpx;border-radius:8rpx;color:#fff;background:#2879e8}.composer-icon{margin:0;width:42rpx;height:42rpx;color:#fff;background:rgba(255,255,255,.16)}.composer-button,.primary,.danger{padding:15rpx 18rpx;border-radius:6rpx;background:#fff;color:#2879e8;font-size:22rpx;font-weight:800}.answer-tabs{display:flex;gap:38rpx;padding:0 6rpx;border-bottom:1rpx solid #e5eaf0}.answer-tabs .active{border-bottom:5rpx solid #2879e8;color:#2879e8;font-weight:800}.question-card{margin-top:18rpx;padding:26rpx}.question-meta,.post-meta{gap:10rpx;color:#718096;font-size:21rpx}.mini-avatar,.avatar{display:grid;place-items:center;flex:none;border-radius:50%;color:#fff;background:#9ab2d5;font-weight:800}.mini-avatar{width:46rpx;height:46rpx;font-size:21rpx}.question-bottom{display:flex;align-items:center;justify-content:space-between;margin-top:20rpx}.secondary{padding:12rpx 16rpx;border-radius:6rpx;color:#2879e8;background:#eaf3ff;font-size:22rpx;font-weight:700}.filter-section{padding-top:26rpx}.chips{white-space:nowrap}.chips .active{color:#fff;background:#2879e8;font-weight:700}.topic-chips{margin-top:16rpx}.feed{overflow:hidden}.post{padding:28rpx;border-bottom:1rpx solid #e5eaf0}.post:last-child{border:0}.post-who{color:#4a5d74;font-weight:700}.post-time{margin-left:auto}.post-copy{color:#62738a;font-size:23rpx;line-height:1.55}.post-actions{gap:26rpx;margin-top:20rpx;color:#718096;font-size:21rpx}.empty{padding:38rpx 28rpx;color:#718096;text-align:center}.team-hero{padding:30rpx;border:0;color:#fff;background:#1776cf}.hero-top{align-items:flex-start;justify-content:space-between;gap:16rpx}.hero-pill{padding:7rpx 10rpx!important;border-radius:5rpx;color:#fff!important;background:rgba(255,255,255,.18)}.team-code{display:flex;align-items:end;justify-content:space-between;margin-top:28rpx;padding-top:22rpx;border-top:1rpx solid rgba(255,255,255,.23)}.member-row,.setting-row{min-height:104rpx;gap:18rpx;padding:0 28rpx;border-bottom:1rpx solid #e5eaf0}.member-row:last-child,.setting-row:last-child{border:0}.setting-row{justify-content:space-between}.profile-head{gap:20rpx}.avatar{width:88rpx;height:88rpx;background:#4d92dd;font-size:30rpx}.stat-grid,.task-stats{display:grid;grid-template-columns:repeat(3,1fr);margin-top:28rpx;padding-top:24rpx;border-top:1rpx solid #e5eaf0}.moderation{justify-content:space-between;gap:16rpx;padding:24rpx;border-color:#fde2a6;background:#fff4db}.logout{width:100%;height:88rpx;border:1rpx solid #ffd8dc;border-radius:8rpx;color:#c23742;background:#fff5f6;font-size:26rpx;font-weight:700}.bottom-nav{position:fixed;z-index:10;bottom:0;left:0;right:0;height:120rpx;padding:0 20rpx env(safe-area-inset-bottom);display:flex;align-items:center;justify-content:space-around;border-top:1rpx solid #e5eaf0;background:rgba(255,255,255,.98)}.bottom-nav .active{color:#2879e8;font-weight:800}.bottom-nav .active:after{position:absolute;bottom:22rpx;width:28rpx;height:5rpx;border-radius:5rpx;background:#2879e8;content:''}.mask{position:fixed;z-index:30;inset:0;display:flex;align-items:flex-end;background:rgba(22,36,58,.42)}.sheet{box-sizing:border-box;width:100%;max-height:82vh;padding:30rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));overflow:auto;border-radius:16rpx 16rpx 0 0;background:#fff}.sheet-head{justify-content:space-between;padding-bottom:22rpx;border-bottom:1rpx solid #e5eaf0;font-size:31rpx}.sheet-body{padding-top:26rpx}.detail-copy{margin-bottom:24rpx;color:#60728a;font-size:24rpx;line-height:1.65}.detail-list{overflow:hidden;border:1rpx solid #e5eaf0;border-radius:8rpx}.detail-row{min-height:88rpx;justify-content:space-between;gap:18rpx;padding:16rpx 22rpx;border-bottom:1rpx solid #e5eaf0}.detail-row:last-child{border:0}.primary,.danger{display:block;width:100%;margin-top:24rpx;padding:18rpx;color:#fff;background:#2879e8}.danger{background:#c23742}.sheet .task-stats{margin:0 0 24rpx;padding-bottom:24rpx}
/* Keep the mobile rhythm of the HTML reference: 20px section gutters and a 72px navigation bar. */
.section{padding-left:40rpx;padding-right:40rpx;padding-top:40rpx}.page-head,.profile-card{padding:40rpx}.bottom-nav{height:144rpx}.community{padding-bottom:calc(172rpx + env(safe-area-inset-bottom))}.safe-top{height:calc(env(safe-area-inset-top) + 12rpx);min-height:48rpx;background:#fff}.notice-button{display:grid;place-items:center;width:68rpx;height:68rpx;padding:0}.bell-icon{position:relative;width:24rpx;height:26rpx;border:3rpx solid currentColor;border-bottom:0;border-radius:14rpx 14rpx 4rpx 4rpx}.bell-icon:before{position:absolute;right:-6rpx;bottom:-5rpx;left:-6rpx;height:3rpx;border-radius:3rpx;background:currentColor;content:''}.bell-icon:after{position:absolute;bottom:-10rpx;left:7rpx;width:7rpx;height:7rpx;border-radius:50%;background:currentColor;content:''}.nav-icon{position:relative;box-sizing:border-box;width:28rpx;height:28rpx;color:currentColor}.home-icon{width:25rpx;height:22rpx;margin-top:5rpx;border:3rpx solid currentColor;border-radius:4rpx}.home-icon:before{position:absolute;top:-10rpx;left:1rpx;width:16rpx;height:16rpx;border-top:3rpx solid currentColor;border-left:3rpx solid currentColor;transform:rotate(45deg);content:''}.qa-icon{height:22rpx;border:3rpx solid currentColor;border-radius:7rpx}.qa-icon:after{position:absolute;bottom:-6rpx;left:5rpx;width:8rpx;height:8rpx;border-bottom:3rpx solid currentColor;border-left:3rpx solid currentColor;transform:skewY(-35deg);content:''}.team-icon{border:3rpx solid currentColor;border-radius:50%}.team-icon:before{position:absolute;top:5rpx;left:7rpx;width:8rpx;height:8rpx;border:3rpx solid currentColor;border-radius:50%;content:''}.team-icon:after{position:absolute;right:2rpx;bottom:3rpx;left:2rpx;height:7rpx;border:3rpx solid currentColor;border-radius:10rpx 10rpx 0 0;border-bottom:0;content:''}.profile-icon{border:3rpx solid currentColor;border-radius:50%}.profile-icon:before{position:absolute;top:4rpx;left:7rpx;width:8rpx;height:8rpx;border:3rpx solid currentColor;border-radius:50%;content:''}.profile-icon:after{position:absolute;right:3rpx;bottom:3rpx;left:3rpx;height:7rpx;border:3rpx solid currentColor;border-radius:10rpx 10rpx 0 0;border-bottom:0;content:''}.nav-badge{position:absolute;top:18rpx;right:calc(50% - 40rpx);min-width:28rpx;height:28rpx;padding:0 5rpx;border-radius:20rpx;color:#fff!important;background:#e4515b;font-size:17rpx!important;text-align:center;line-height:28rpx}.status-card{background:linear-gradient(135deg,#eef7ff,#f7fbff)}.bottom-nav .nav-icon{position:relative!important;box-sizing:border-box;display:block!important;flex:none!important;width:28rpx!important;height:28rpx!important;color:currentColor}.home-head{display:flex;align-items:center;gap:24rpx}.home-head .notice-button{flex:none}.nav-badge{display:none!important}.post{padding:38rpx!important}.post-title{margin-top:24rpx}.post-body{margin-top:24rpx;padding:22rpx;border-radius:8rpx;background:#f5f8fb}.post-body .post-copy{font-size:27rpx!important;line-height:1.7!important}.post-actions{gap:16rpx;margin-top:28rpx!important}.post-actions .report-button{color:#c23742!important;background:#fff0f1!important}
.bottom-nav{height:144rpx!important;padding:0 20rpx env(safe-area-inset-bottom)!important}.nav-item{position:relative;display:flex;flex:1;flex-direction:column;align-items:center;justify-content:center;gap:6rpx;height:100%;color:#7a899a;font-size:23rpx;line-height:1.2}.nav-item.active{color:#2879e8;font-weight:800}.nav-item.active:after{position:absolute;bottom:16rpx;width:30rpx;height:5rpx;border-radius:5rpx;background:#2879e8;content:''}.nav-icon{position:relative!important;box-sizing:border-box;display:block!important;flex:none!important;width:40rpx!important;height:40rpx!important;color:currentColor}.home-icon{width:32rpx!important;height:28rpx!important;margin-top:8rpx}.qa-icon{width:36rpx!important;height:30rpx!important}.profile-icon{width:38rpx!important;height:38rpx!important}.team-icon{border:0!important;border-radius:0!important}.team-icon:before{position:absolute;top:2rpx;left:1rpx;width:12rpx;height:12rpx;border:3rpx solid currentColor;border-radius:50%;box-shadow:18rpx 5rpx 0 -1rpx #fff,18rpx 5rpx 0 2rpx currentColor;content:''}.team-icon:after{position:absolute;bottom:1rpx;left:0;width:20rpx;height:12rpx;border:3rpx solid currentColor;border-radius:14rpx 14rpx 0 0;border-bottom:0;box-shadow:18rpx 5rpx 0 -1rpx #fff,18rpx 5rpx 0 2rpx currentColor;content:''}.subhead,.card-copy,.post-copy,.detail-copy{display:block}.answer-tabs{display:flex;gap:40rpx;padding:0 8rpx;border-bottom:1rpx solid #e5eaf0}.chips{white-space:nowrap}.chip{display:inline-block;margin-right:14rpx;padding:14rpx 20rpx;border-radius:6rpx;color:#60728a;background:#f1f5f9;font-size:24rpx}.chip.active{color:#fff;background:#2879e8;font-weight:800}.answer-tabs .active{padding:16rpx 0 13rpx}.post-title{margin-top:24rpx;font-size:34rpx;font-weight:800;line-height:1.48}.post-body{margin-top:24rpx;padding:22rpx;border-radius:8rpx;background:#f5f8fb}.post-actions{display:flex;flex-wrap:wrap;gap:16rpx;margin-top:28rpx}.action-button{min-height:64rpx;padding:15rpx 20rpx;border-radius:6rpx;color:#2879e8;background:#eaf3ff;font-size:25rpx;font-weight:800;line-height:1.2}.report-button{color:#c23742;background:#fff0f1}
/* Unified touch targets: 88rpx for primary actions and 76rpx for compact post actions. */
.notice-button{width:88rpx!important;height:88rpx!important}
.composer-button,.copy-button,.moderation-button{box-sizing:border-box;display:flex;align-items:center;justify-content:center;min-width:112rpx;min-height:80rpx;margin:0;padding:0 24rpx;border-radius:8rpx;font-size:26rpx;font-weight:800;line-height:1.2}
.composer-button{color:#2879e8;background:#fff}
.copy-button{color:#2879e8;background:#fff}
.moderation-button{flex:none;color:#9a6500;background:#fff}
.sheet-close-button{box-sizing:border-box;display:flex;align-items:center;justify-content:center;min-width:96rpx;min-height:64rpx;margin:0;padding:0 16rpx;border-radius:8rpx;color:#52657e;background:#f2f6fb;font-size:24rpx;font-weight:700;line-height:1.2}
.primary,.danger{box-sizing:border-box;min-height:88rpx;padding:0 28rpx;font-size:28rpx;line-height:1.2}
.action-button{box-sizing:border-box;min-height:76rpx;padding:0 24rpx;font-size:26rpx;line-height:1.2}
.chip{box-sizing:border-box;min-height:72rpx;padding:0 22rpx;font-size:25rpx;line-height:72rpx}
.answer-tabs{min-height:76rpx;align-items:stretch}
.answer-tabs .active{box-sizing:border-box;display:flex;align-items:center;padding:0;border-bottom-width:5rpx}
.nav-item{gap:8rpx;padding-top:6rpx;box-sizing:border-box}
.report-post{display:flex;flex-direction:column;gap:12rpx;padding:22rpx;border-radius:8rpx;background:#f5f8fb;color:#17243a;font-size:27rpx;line-height:1.45}.report-reasons{display:flex;flex-wrap:wrap;gap:16rpx;margin-top:24rpx}.report-reason{box-sizing:border-box;min-height:72rpx;padding:0 22rpx;border:1rpx solid #dce5ef;border-radius:8rpx;color:#52657e;background:#fff;font-size:25rpx;line-height:70rpx}.report-reason.active{border-color:#c23742;color:#c23742;background:#fff0f1;font-weight:700}.report-textarea{box-sizing:border-box;width:100%;height:172rpx;margin-top:24rpx;padding:20rpx;border:1rpx solid #dce5ef;border-radius:8rpx;background:#fff;color:#17243a;font-size:25rpx;line-height:1.5}
.primary,.danger,.action-button,.logout{display:flex;align-items:center;justify-content:center;padding-top:0!important;padding-bottom:0!important}
.team-error{display:flex;align-items:center;justify-content:space-between;gap:16rpx;padding:24rpx;border:1rpx solid #fde2a6;border-radius:8rpx;background:#fff4db;color:#9a6500;font-size:24rpx;line-height:1.5}.team-error__retry{flex:none;color:#2879e8;font-weight:800}.team-error__hint{display:block;margin-top:12rpx;color:#8a97a5;font-size:22rpx;line-height:1.5}.pending-post{padding:22rpx 0;border-bottom:1rpx solid #e5eaf0}.pending-post__title{display:block;font-size:27rpx;font-weight:800;line-height:1.45}.pending-post__copy{display:block;margin-top:10rpx;color:#62738a;font-size:23rpx;line-height:1.55}.pending-post__actions{display:flex;gap:16rpx;margin-top:16rpx}.pending-post__btn{width:auto!important;min-width:120rpx;margin:0!important;padding:12rpx 24rpx!important;font-size:24rpx!important}.pending-post__approve{color:#fff!important}.pending-post__empty{margin-top:20rpx}
.answer-tab{box-sizing:border-box;display:flex;align-items:center;min-height:76rpx;padding:0;font-size:26rpx;line-height:1.2}
.stat-grid{gap:0;padding-top:28rpx}
.stat-item{position:relative;display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:104rpx;text-align:center}
.stat-item+.stat-item:before{position:absolute;top:10rpx;bottom:10rpx;left:0;width:1rpx;background:#dce5ef;content:''}
.stat-number{display:block;color:#17243a;font-size:34rpx;font-weight:800;line-height:1.25}
.stat-label{display:block;margin-top:8rpx;color:#60728a;font-size:24rpx;line-height:1.3}
.qa-head{display:flex;align-items:center;gap:24rpx}
.qa-post-button{box-sizing:border-box;display:flex;align-items:center;justify-content:center;flex:none;min-width:112rpx;min-height:80rpx;margin:0;padding:0 24rpx;border-radius:8rpx;color:#fff;background:#2879e8;font-size:26rpx;font-weight:800;line-height:1.2}
.detail-copy{white-space:pre-wrap}
.discussion-page{position:fixed;z-index:60;inset:0;display:flex;flex-direction:column;background:#fff}.discussion-head{display:flex;align-items:center;justify-content:space-between;min-height:104rpx;padding:0 32rpx;border-bottom:1rpx solid #edf0f2;background:#fff;font-size:30rpx}.discussion-back,.discussion-spacer{width:96rpx}.discussion-back{box-sizing:border-box;min-height:64rpx;margin:0;padding:0;border-radius:32rpx;color:#53606b;background:#f5f6f7;font-size:24rpx;font-weight:700}.discussion-content{flex:1;box-sizing:border-box;padding:56rpx 40rpx calc(152rpx + env(safe-area-inset-bottom));background:#fff}.discussion-post{padding:0 0 34rpx;border-bottom:1rpx solid #edf0f2}.discussion-author,.discussion-copy{display:block;color:#8c959d;font-size:24rpx;line-height:1.55}.discussion-title{display:block;margin:18rpx 0 10rpx;font-size:36rpx;line-height:1.45}.discussion-section-head{display:flex;align-items:center;justify-content:center;margin:34rpx 0 10rpx;color:#222;font-size:29rpx}.discussion-section-head .comment-count{color:#8c959d;font-size:24rpx}.discussion-comments{background:#fff}.comment-thread{border-bottom:1rpx solid #f0f1f2}.comment-thread:last-child{border-bottom:0}.comment-row{display:flex;position:relative;gap:18rpx;padding:28rpx 0 22rpx}.comment-avatar{display:flex;align-items:center;justify-content:center;flex:none;width:72rpx;height:72rpx;border-radius:50%;color:#fff;background:#79c7d3;font-size:25rpx;font-weight:700}.reply-avatar{width:60rpx;height:60rpx;background:#b5c2d1;font-size:22rpx}.comment-main{flex:1;min-width:0}.comment-author-line{display:flex;align-items:center;gap:10rpx}.comment-author{display:block;color:#9aa1a8;font-size:25rpx;line-height:1.25}.comment-copy{display:block;margin-top:10rpx;color:#202428;font-size:30rpx;line-height:1.5;word-break:break-all}.comment-foot{display:flex;align-items:center;gap:22rpx;margin-top:14rpx}.comment-meta{color:#a1a7ad;font-size:22rpx;line-height:1.2}.comment-action{display:inline-flex;align-items:center;min-height:40rpx;margin:0;padding:0;color:#777f86;font-size:23rpx;line-height:1.2}.reply-action{color:#68727b}.like-action{display:inline-flex;align-items:center;justify-content:flex-end;flex:none;margin-left:auto;color:#8f979f;font-size:26rpx;line-height:1.2;text-align:right}.like-action.liked{color:#e36b78}.identity-badge{display:inline-flex;align-items:center;min-height:32rpx;padding:0 9rpx;border-radius:4rpx;color:#2879e8;background:#edf5ff;font-size:20rpx;line-height:32rpx;white-space:nowrap}.identity-label{color:#84909a;font-size:22rpx;line-height:1.2}.identity-options,.identity-picker{display:flex;align-items:center;gap:10rpx;flex-wrap:wrap}.compose-identity{padding:18rpx 20rpx;border-radius:8rpx;background:#f5f8fb}.compose-identity .identity-label{display:block;margin-bottom:14rpx}.identity-option{display:inline-flex;align-items:center;justify-content:center;min-height:52rpx;padding:0 16rpx;border:1rpx solid #dce5ef;border-radius:26rpx;color:#687681;background:#fff;font-size:22rpx;line-height:1.2}.identity-option.active{border-color:#2879e8;color:#2879e8;background:#edf5ff;font-weight:700}.identity-picker{flex:none;width:100%;margin-bottom:10rpx}.identity-picker .identity-label{margin-right:2rpx}.reply-prefix{color:#7f8790;font-size:27rpx}.reply-summary{display:flex;align-items:center;gap:12rpx;margin:0 0 22rpx 90rpx;color:#8e969d;font-size:24rpx}.reply-line{width:34rpx;height:1rpx;background:#d8dde0}.reply-chevron{font-size:29rpx;line-height:1;transform:translateY(-3rpx)}.nested-comments{margin-left:90rpx}.nested-comments .comment-row{padding-top:18rpx}.discussion-empty{padding:48rpx 28rpx;color:#8c959d;text-align:center}.reply-bar{position:fixed;right:0;bottom:0;left:0;z-index:61;padding:16rpx 32rpx calc(16rpx + env(safe-area-inset-bottom));border-top:1rpx solid #edf0f2;background:#fff}.reply-target{display:flex;align-items:center;justify-content:space-between;height:46rpx;color:#73808a;font-size:23rpx}.reply-cancel{color:#2879e8}.reply-entry{display:flex;gap:16rpx;align-items:center;flex-wrap:wrap}.reply-input{box-sizing:border-box;flex:1;height:76rpx;padding:0 28rpx;border:0;border-radius:38rpx;background:#f3f4f5;font-size:26rpx}.reply-button{box-sizing:border-box;display:flex;align-items:center;justify-content:center;min-width:104rpx;height:64rpx;margin:0;padding:0 20rpx;border:0;border-radius:32rpx;color:#fff;background:#2879e8;font-size:25rpx;font-weight:800}.comment-report-mask{position:fixed;z-index:70;inset:0;display:flex;align-items:flex-end;background:rgba(22,36,58,.42)}.comment-report-sheet{box-sizing:border-box;width:100%;padding:30rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));border-radius:16rpx 16rpx 0 0;background:#fff}
.discussion-author-line{display:flex;align-items:center;gap:10rpx}
.discussion-safe-top{height:env(safe-area-inset-top);min-height:48rpx;background:#fff}
.linked-list-mask{position:fixed;z-index:50;inset:0;display:flex;align-items:flex-end;background:rgba(22,36,58,.42)}.linked-list-sheet{box-sizing:border-box;width:100%;max-height:82vh;padding:30rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));overflow:auto;border-radius:16rpx 16rpx 0 0;background:#fff}.linked-list{margin-top:18rpx;overflow:hidden;border:1rpx solid #e5eaf0;border-radius:8rpx}.linked-list-row{position:relative;padding:24rpx 64rpx 24rpx 22rpx;border-bottom:1rpx solid #e5eaf0}.linked-list-row:last-child{border-bottom:0}.linked-list-row b,.linked-list-row text{display:block}.linked-list-row b{font-size:27rpx;line-height:1.45}.linked-list-row text{margin-top:8rpx;color:#718096;font-size:22rpx;line-height:1.4}.linked-list-row .linked-list-arrow{position:absolute;top:50%;right:24rpx;margin:0;transform:translateY(-50%);color:#2879e8;font-size:38rpx}
</style>
