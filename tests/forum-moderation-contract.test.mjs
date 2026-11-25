import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('student forum sends identity, university, category and sort filters', () => {
  const source = read('src/pages/student/qa-forum.vue')
  assert.match(source, /getForumPosts\([\s\S]*authorType:[\s\S]*universityId:[\s\S]*category:[\s\S]*sort:/)
})

test('forum posts require moderation after sensitive word and mute checks', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/QAService.java')
  assert.match(source, /assertNotMuted\(askerId, askerType\)/)
  assert.match(source, /validateContent\(dto\.getTitle\(\), dto\.getQuestionContent\(\)\)/)
  assert.match(source, /qa\.setAuditStatus\("PENDING"\)[\s\S]*qa\.setIsPublic\(0\)/)
})

test('mute escalation starts at one hour and caps at thirty days', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /case 1 -> Duration\.ofHours\(1\)/)
  assert.match(source, /case 2 -> Duration\.ofHours\(6\)/)
  assert.match(source, /default -> Duration\.ofDays\(30\)/)
})

test('moderator scope and self-review restrictions are enforced server-side', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(source, /只能处理本校帖子/)
  assert.match(source, /不能审核或设置自己的帖子/)
  assert.match(source, /原举报处理人不能复核申诉/)
})

test('multiple reports for one post do not stack duplicate penalties', () => {
  const source = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  // 一帖一次处罚：首个成立举报创建处罚，后续成立举报关联同一处罚（不叠加处罚、可追溯申诉）
  assert.match(source, /priorFounded[\s\S]*if \(priorFounded != null\)[\s\S]*penaltyId = priorFounded\.getPenaltyId\(\)/)
})

test('forum migration contains report, penalty, appeal and moderator grant tables', () => {
  const source = read('backend/src/main/resources/db/forum_moderation.sql')
  for (const table of ['forum_moderator_grant', 'forum_report', 'forum_penalty', 'forum_appeal', 'forum_sensitive_word']) {
    assert.match(source, new RegExp(`CREATE TABLE IF NOT EXISTS ${table}`))
  }
})

test('report moderation includes the reported post title and content', () => {
  const entity = read('backend/src/main/java/com/demo/miniprogram/entity/ForumReport.java')
  const mapper = read('backend/src/main/java/com/demo/miniprogram/mapper/ForumReportMapper.java')
  const page = read('src/pages/volunteer/forum-moderation.vue')
  assert.match(entity, /private String postTitle/)
  assert.match(entity, /private String postContent/)
  assert.match(mapper, /qa\.title AS post_title/)
  assert.match(mapper, /qa\.question_content AS post_content/)
  assert.match(page, /item\.postTitle/)
  assert.match(page, /item\.postContent/)
})

test('featured posts are filtered by the backend and expose a visible marker', () => {
  const controller = read('backend/src/main/java/com/demo/miniprogram/controller/QAController.java')
  const service = read('backend/src/main/java/com/demo/miniprogram/service/QAService.java')
  const mapper = read('backend/src/main/java/com/demo/miniprogram/mapper/QuestionAnswerMapper.java')
  const item = read('src/components/QuestionItem.vue')
  const detail = read('src/pages/common/qa-detail.vue')
  assert.match(controller, /listFeaturedQuestions\(queryDTO\)/)
  assert.match(service, /selectForumPosts\(page, null, null, null, 1, "featured"\)/)
  assert.match(mapper, /<if test='featured != null and featured == 1'> AND qa\.is_featured = 1 <\/if>/)
  assert.match(item, /data\.isFeatured[^\n]*>精华/)
  assert.match(detail, /post\.isFeatured[^\n]*>精华/)
})
