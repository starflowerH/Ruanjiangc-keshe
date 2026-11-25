import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

// ============ P0-1 / P1-1：一对一私密问答与隐藏帖的可见性隔离 ============

test('QA detail no longer grants admin-wide bypass and requires SUPER_ADMIN explicitly', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/mapper/QuestionAnswerMapper.java')
  // 旧的 admin 通读已移除
  assert.doesNotMatch(source, /#{userType} = 'admin'/)
  // 超级管理员显式放行
  assert.match(source, /#{isSuperAdmin} = 1/)
  // 一对一参与者可读 PENDING/APPROVED（REJECTED 除外；XML 转义 &lt;&gt; 即 SQL 的 <>）
  assert.match(source, /qa\.asker_student_id = #{relatedId} AND qa\.audit_status &lt;&gt; 'REJECTED'/)
})

test('hot questions and favorites require is_public=1 and exclude one-to-one posts', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/mapper/QuestionAnswerMapper.java')
  const hot = source.slice(source.indexOf('今日热搜'), source.indexOf('论坛帖子列表'))
  assert.match(hot, /target_volunteer_id IS NULL AND qa\.audit_status = 'APPROVED' AND qa\.is_public = 1/)
  assert.match(source, /我的收藏帖子分页[\s\S]*qa\.audit_status = 'APPROVED' AND qa\.is_public = 1 AND qa\.target_volunteer_id IS NULL/)
})

test('comments are only served for public forum posts', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumCommentService.java')
  assert.match(source, /post\.getTargetVolunteerId\(\) != null[\s\S]*throw new BusinessException\("帖子不存在或已下架"\)/)
})

// ============ P1-2：一对一待审核流程 ============

test('answering a one-to-one question approves it for the asker without making it public', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/QAService.java')
  // 条件更新中设置 APPROVED（保持私密 is_public 不变，仅参与者可见）
  assert.match(source, /\.set\(QuestionAnswer::getAuditStatus, "APPROVED"\)/)
  assert.match(source, /\.isNull\(QuestionAnswer::getAnswerContent\)/)
})

// ============ P1-3 / P1-6：状态机与并发 ============

test('post review uses conditional update on PENDING', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /\.eq\(QuestionAnswer::getId, postId\)\s*\.eq\(QuestionAnswer::getAuditStatus, "PENDING"\)/)
  assert.match(source, /if \(affected == 0\)[\s\S]*帖子已被审核或状态已变化/)
})

test('report and appeal reviews use conditional updates on PENDING', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /\.eq\(ForumReport::getStatus, "PENDING"\)/)
  assert.match(source, /\.eq\(ForumAppeal::getStatus, "PENDING"\)/)
})

test('penalty counting locks the user row to serialize concurrent reviews', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /selectByIdForUpdate\(userId\)/)
  const volunteerMapper = read('backend/src/main/java/com/demo/miniprogram/mapper/VolunteerMapper.java')
  assert.match(volunteerMapper, /FOR UPDATE/)
})

// ============ P1-4 / P1-5：授权联动与主体判断 ============

test('cancelling a volunteer deactivates forum moderator grants', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/VolunteerService.java')
  assert.match(source, /forumModeratorGrantMapper\.update\(null, new LambdaUpdateWrapper<com\.demo\.miniprogram\.entity\.ForumModeratorGrant>\(\)[\s\S]*\.set\(com\.demo\.miniprogram\.entity\.ForumModeratorGrant::getActive, 0\)/)
})

test('grant check re-validates volunteer APPROVED status in real time', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /!"APPROVED"\.equals\(volunteer\.getStatus\(\)\)[\s\S]*return null/)
})

test('self-review and appeal re-check compare by openid subject', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /sameOpenidAsAuthor\(post, reviewerOpenid\)/)
  assert.match(source, /isSameSubject\(report\.getReviewerId\(\), report\.getReviewerType\(\), reviewerId, userType\)/)
})

// ============ P1-7 / P1-8 / H4：权限边界与鉴权 ============

test('admin stats no longer allow TEACHER', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/controller/AdminController.java')
  assert.match(source, /@RequireRole\(\{"SUPER_ADMIN", "NORMAL_ADMIN"\}\)\s*\n\s*public Result<Map<String, Object>> getStats/)
})

test('JWT filter checks admin account status and normalizes traversal paths', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/config/JwtAuthFilter.java')
  assert.match(source, /adminUserMapper\.selectById\(relatedId\)[\s\S]*账号已禁用或不存在/)
  assert.match(source, /normalizePath\(/)
  assert.match(source, /while \(result\.contains\("\/\.\.\/"\)\)/)
})

// ============ P2：治理参数、精华审计、通知、幂等 ============

test('forum moderation endpoints use validated DTOs instead of raw Maps', () => {
  for (const c of ['ForumAdminController.java', 'ForumModerationController.java']) {
    const source = read(`backend/src/main/java/com/demo/miniprogram/controller/${c}`)
    assert.doesNotMatch(source, /@RequestBody Map<String, (String|Object)> body/)
    assert.match(source, /@Valid @RequestBody ForumReviewActionDTO/)
    assert.match(source, /@Valid @RequestBody ForumReportReviewDTO/)
    assert.match(source, /@Valid @RequestBody ForumAppealReviewDTO/)
  }
})

test('setFeatured records the operator and time', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /post\.setFeaturedByType\(userType\)[\s\S]*post\.setFeaturedById\(reviewerId\)[\s\S]*post\.setFeaturedAt\(LocalDateTime\.now\(\)\)/)
})

test('moderation actions notify the author', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /notificationService\.createSystemNotification/)
})

test('forum migration script is idempotent', () => {
  const source = read('backend/src/main/resources/db/forum_moderation.sql')
  assert.match(source, /information_schema\.COLUMNS/)
  assert.match(source, /CREATE TABLE IF NOT EXISTS forum_report/)
  assert.match(source, /featured_by_type/)
})

// ============ P2-10 / P2-11 / P2-12 / P2-13：契约与前端 ============

test('toggleQAPublic uses query param on the frontend to match @RequestParam backend', () => {
  const source = read('src/api/admin.js')
  assert.match(source, /toggle-public\?isPublic=\$\{isPublic\}/)
})

test('custom-navbar pages declare navigationStyle custom in pages.json', () => {
  const source = read('src/pages.json')
  assert.match(source, /"path": "pages\/student\/ask-question"[\s\S]{0,200}"navigationStyle": "custom"/)
  assert.match(source, /"path": "pages\/common\/qa-detail"[\s\S]{0,200}"navigationStyle": "custom"/)
  assert.match(source, /"path": "pages\/common\/my-favorites"[\s\S]{0,200}"navigationStyle": "custom"/)
})

test('submit handlers guard against double-submission', () => {
  for (const p of ['src/pages/student/ask-question.vue', 'src/pages/volunteer/answer-question.vue', 'src/pages/common/qa-detail.vue']) {
    const source = read(p)
    assert.match(source, /submitting/)
  }
})

test('frontend derives expired penalty state instead of showing active forever', () => {
  const source = read('src/pages/common/forum-penalties.vue')
  assert.match(source, /已失效/)
  assert.match(source, /isExpired\(item\)/)
})

// ============ P3：安全配置 ============

test('production config avoids stdout SQL, DEBUG logs and default PII', () => {
  const source = read('backend/src/main/resources/application.yml')
  assert.doesNotMatch(source, /StdOutImpl/)
  assert.match(source, /Slf4jImpl/)
  assert.match(source, /send-default-pii: false/)
  assert.doesNotMatch(source, /com\.demo\.miniprogram: DEBUG/)
})

test('upload validates image magic bytes', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/controller/UploadController.java')
  assert.match(source, /isValidImageMagic/)
})

// ============ 自审回归修复（2026-08-12 第二轮） ============

test('comment names resolve legacy wechat-type comments', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumCommentService.java')
  assert.match(source, /"wechat"\.equals\(c\.getUserType\(\)\)/)
  assert.match(source, /result\.put\(id \+ ":wechat"/)
})

test('answer uses conditional update to prevent concurrent double-answer', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/QAService.java')
  assert.match(source, /\.isNull\(QuestionAnswer::getAnswerContent\)[\s\S]*\.set\(QuestionAnswer::getAnswerContent, dto\.getAnswerContent\(\)\)/)
})

test('moderation pagination clamps page and size', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /sanitizeSize\(size\)[\s\S]*Math\.min\(Math\.max\(size, 1\), 100\)/)
})

test('penalty linking uses current-read FOR UPDATE to avoid duplicate penalties', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /LIMIT 1 FOR UPDATE/)
})

test('JWT filter decodes percent-encoded traversal variants', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/config/JwtAuthFilter.java')
  assert.match(source, /replaceAll\("\(\?i\)%2e", "\."\)/)
  assert.match(source, /replaceAll\("\(\?i\)%2f", "\/"\)/)
})

test('custom navbar pages adapt status bar height', () => {
  for (const p of ['src/pages/student/ask-question.vue', 'src/pages/common/qa-detail.vue']) {
    const source = read(p)
    assert.match(source, /var\(--status-bar-height\)/)
  }
})

test('moderation page guards stale words-tab responses', () => {
  const source = read('src/pages/volunteer/forum-moderation.vue')
  assert.match(source, /active\.value === 'words'\)[\s\S]*if \(seq !== requestSeq\) return/)
})

// ============ 线上事故回归防护（2026-08-12 晚） ============

test('api base config must not use import.meta (vite injects require("url") shim that crashes mini-program runtime)', () => {
  const source = read('src/api/index.js')
  assert.doesNotMatch(source, /import\.meta/)
  assert.doesNotMatch(source, /process\.env/)
  assert.match(source, /from '@\/config\/env'/)
  const envConfig = read('src/config/env.js')
  assert.match(envConfig, /BASE_URL = 'https:\/\/hynu\.site'/)
})

test('no other source file uses import.meta or process.env at runtime', () => {
  const files = ['src/store/modules/user.js', 'src/utils/auth.js', 'src/utils/storage.js', 'src/utils/report.js', 'src/utils/nav.js']
  for (const f of files) {
    const source = read(f)
    assert.doesNotMatch(source, /import\.meta|process\.env/, `${f} must not reference import.meta/process.env`)
  }
})

test('unmatched routes return 404 instead of ERROR-level Sentry noise', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/common/GlobalExceptionHandler.java')
  assert.match(source, /NoResourceFoundException/)
})

// ============ 老师注册审核闭环（2026-08-12 晚） ============

test('admin api exposes teacher list and enable endpoints', () => {
  const source = read('src/api/admin.js')
  assert.match(source, /export function getTeacherList\(\)[\s\S]*\/api\/admin\/teachers'/)
  assert.match(source, /export function enableTeacher\(id, enabled\)[\s\S]*\/api\/admin\/teachers\/\$\{id\}\/enable/)
})

test('super admin dashboard has teacher-audit entry and page is registered', () => {
  const dashboard = read('src/pages/admin/dashboard.vue')
  assert.match(dashboard, /pages\/admin\/teacher-audit/)
  const pages = read('src/pages.json')
  assert.match(pages, /"path": "pages\/admin\/teacher-audit"[\s\S]{0,120}"navigationStyle": "custom"/)
  const page = read('src/pages/admin/teacher-audit.vue')
  assert.match(page, /getTeacherList\(\)/)
  assert.match(page, /enableTeacher\(item\.id, enabled\)/)
})

test('teacher registration does not fake a login session when backend returns no token', () => {
  const source = read('src/pages/teacher/login.vue')
  assert.match(source, /if \(result\.token\)[\s\S]*saveAndRedirect\(result\)[\s\S]*else[\s\S]*等待系统管理员审核启用/)
})

// ============ 老师端退出闭环（2026-08-12 夜） ============

test('teacher logout returns to identity selection like other roles', () => {
  const source = read('src/pages/teacher/home.vue')
  assert.match(source, /title: '提示'[\s\S]*确定要退出登录吗？[\s\S]*reLaunch\(\{ url: '\/pages\/index\/index' \}\)/)
})

test('teacher login page is not a dead end (has back-to-home entry)', () => {
  const source = read('src/pages/teacher/login.vue')
  assert.match(source, /返回身份选择[\s\S]*goHome/)
  assert.match(source, /function goHome\(\)[\s\S]*reLaunch\(\{ url: '\/pages\/index\/index' \}\)/)
})

test('teacher registration success returns to identity selection, not trapped login page', () => {
  const source = read('src/pages/common/login.vue')
  assert.match(source, /注册成功，请等待管理员审核启用后登录[\s\S]*reLaunch\(\{ url: '\/pages\/index\/index' \}\)/)
})

// ============ 被取消志愿者身份恢复（2026-08-14） ============

test('login restores volunteer identity context regardless of status (CANCELLED included)', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/AuthService.java')
  assert.match(source, /findLatestVolunteerAny\(openid\)/)
  assert.match(source, /不限状态取最新报名记录/)
})

test('legacy JWT fallback resolves volunteer by any status to avoid identity-space confusion', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/config/JwtAuthFilter.java')
  assert.match(source, /\.eq\(Volunteer::getOpenid, openid\)\s*\.orderByDesc\(Volunteer::getUpdatedAt\)/)
})

test('cancelled volunteers see a re-apply entry on their home card', () => {
  const source = read('src/pages/volunteer/home.vue')
  assert.match(source, /status === 'CANCELLED'[\s\S]*重新报名/)
})

test('team page explains abnormal registration status instead of raw status code', () => {
  const source = read('src/pages/volunteer/team-view.vue')
  assert.match(source, /function statusExplain\(status\)/)
  assert.match(source, /您的志愿者资格已被取消，无法参与组队/)
  assert.match(source, /您的报名未通过审核/)
  assert.match(source, /abnormalStatus === 'CANCELLED' \? '重新报名'/)
})
