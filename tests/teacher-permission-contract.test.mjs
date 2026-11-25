import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('super admin can update a teacher permission set through a typed endpoint', () => {
  const controller = read('backend/src/main/java/com/demo/miniprogram/controller/AdminController.java')
  const dto = read('backend/src/main/java/com/demo/miniprogram/dto/TeacherPermissionsDTO.java')
  assert.match(controller, /@PutMapping\("\/teachers\/\{id\}\/permissions"\)/)
  assert.match(controller, /@RequireRole\("SUPER_ADMIN"\)/)
  assert.match(controller, /@Valid @RequestBody TeacherPermissionsDTO body/)
  assert.match(dto, /@NotNull[\s\S]*teacherVolunteerAudit/)
  assert.match(dto, /@NotNull[\s\S]*teacherTeamManage/)
  assert.match(dto, /@NotNull[\s\S]*teacherPromotionManage/)
  assert.match(dto, /@NotNull[\s\S]*teacherForumModeration/)
})

test('teacher permissions are checked from the database on every protected request', () => {
  const aspect = read('backend/src/main/java/com/demo/miniprogram/security/TeacherPermissionAspect.java')
  const service = read('backend/src/main/java/com/demo/miniprogram/service/TeacherPermissionService.java')
  assert.match(aspect, /permissionService\.hasAny\(teacherId, requirePermission\.value\(\)\)/)
  assert.match(service, /adminUserMapper\.selectById\(teacherId\)/)
  assert.match(service, /teacher\.getStatus\(\) != 1/)
})

test('teacher volunteer, team, promotion and forum capabilities are protected', () => {
  const teacher = read('backend/src/main/java/com/demo/miniprogram/controller/TeacherController.java')
  const promotion = read('backend/src/main/java/com/demo/miniprogram/controller/UniversityContentController.java')
  const forum = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  assert.match(teacher, /@RequireTeacherPermission\(TeacherPermissions\.VOLUNTEER_AUDIT\)/)
  assert.match(teacher, /@RequireTeacherPermission\(TeacherPermissions\.TEAM_MANAGE\)/)
  assert.match(promotion, /promotionManagementService\.resolveManagedUniversity/)
  assert.match(forum, /TeacherPermissions\.FORUM_MODERATION/)
  assert.match(forum, /return teacher\.getUniversityId\(\)/)
})

test('new teachers default to no permissions and login returns the permission payload', () => {
  const auth = read('backend/src/main/java/com/demo/miniprogram/service/AuthService.java')
  assert.match(auth, /admin\.setPermissions\("\{\\"teacherVolunteerAudit\\":false/)
  assert.match(auth, /loginVO\.setPermissions\(admin\.getPermissions\(\)\)/)
})

test('teacher audit and home pages expose permission controls and gated entries', () => {
  const audit = read('src/pages/admin/teacher-audit.vue')
  const home = read('src/pages/teacher/home.vue')
  const api = read('src/api/admin.js')
  assert.match(audit, /changePermission\(item, permission\.key/)
  assert.match(audit, /teacherForumModeration/)
  assert.match(api, /PUT[\s\S]*\/api\/admin\/teachers\/\$\{id\}\/permissions/)
  assert.match(home, /v-if="canVolunteerAudit"/)
  assert.match(home, /v-if="canTeamManage"/)
  assert.match(home, /v-if="canPromotionManage"/)
  assert.match(home, /v-if="canForumModeration"/)
})

test('authorized teachers can grant forum moderator rights only within their university', () => {
  const teacher = read('backend/src/main/java/com/demo/miniprogram/controller/TeacherController.java')
  const moderation = read('backend/src/main/java/com/demo/miniprogram/service/ForumModerationService.java')
  const audit = read('src/pages/admin/audit-list.vue')
  const api = read('src/api/admin.js')
  assert.match(teacher, /@PostMapping\("\/forum\/moderators\/\{volunteerId\}"\)/)
  assert.match(teacher, /@RequireTeacherPermission\(TeacherPermissions\.FORUM_MODERATION\)/)
  assert.match(moderation, /allowedUniversityId != null && !allowedUniversityId\.equals\(volunteer\.getUniversityId\(\)\)/)
  assert.match(audit, /canGrantForumModerator/)
  assert.match(api, /\/api\/teacher\/forum\/moderators\/\$\{volunteerId\}/)
})

test('authorized teachers can grant promotion management only within their university', () => {
  const teacher = read('backend/src/main/java/com/demo/miniprogram/controller/TeacherController.java')
  const admin = read('backend/src/main/java/com/demo/miniprogram/controller/AdminController.java')
  const promotion = read('backend/src/main/java/com/demo/miniprogram/service/PromotionManagementService.java')
  const content = read('backend/src/main/java/com/demo/miniprogram/controller/UniversityContentController.java')
  const filter = read('backend/src/main/java/com/demo/miniprogram/config/JwtAuthFilter.java')
  const audit = read('src/pages/admin/audit-list.vue')
  const profile = read('src/pages/volunteer/profile.vue')
  const api = read('src/api/admin.js')

  assert.match(teacher, /@PostMapping\("\/promotion\/managers\/\{volunteerId\}"\)/)
  assert.match(teacher, /@RequireTeacherPermission\(TeacherPermissions\.PROMOTION_MANAGE\)/)
  assert.match(admin, /@PostMapping\("\/promotion\/managers\/\{volunteerId\}"\)[\s\S]*@RequireRole\("SUPER_ADMIN"\)/)
  assert.match(promotion, /allowedUniversityId != null && !allowedUniversityId\.equals\(volunteer\.getUniversityId\(\)\)/)
  assert.match(promotion, /"APPROVED"\.equals\(volunteer\.getStatus\(\)\)/)
  assert.match(content, /resolveManagedUniversity\([\s\S]*saveOrUpdate\(universityId, dto\)/)
  assert.doesNotMatch(content, /@RequireRole\("TEACHER"\)/)
  assert.match(filter, /path\.matches\("\/api\/university-content\//)
  assert.match(audit, /canGrantPromotionManager/)
  assert.match(profile, /isPromotionManager/)
  assert.match(api, /\/api\/teacher\/promotion\/managers\/\$\{volunteerId\}/)
})

test('promotion grants use an idempotent migration and follow volunteer qualification state', () => {
  const migration = read('backend/src/main/resources/db/volunteer_promotion_grant.sql')
  const schema = read('backend/src/main/resources/schema.sql')
  const service = read('backend/src/main/java/com/demo/miniprogram/service/PromotionManagementService.java')
  assert.match(migration, /CREATE TABLE IF NOT EXISTS volunteer_promotion_grant/)
  assert.match(migration, /UNIQUE KEY `uk_volunteer_promotion_manager`/)
  assert.match(schema, /CREATE TABLE volunteer_promotion_grant/)
  assert.match(service, /!"APPROVED"\.equals\(volunteer\.getStatus\(\)\)/)
  assert.match(service, /PromotionManagerGrant::getActive, 1/)
})

test('rejected volunteer applications can be replaced by a new pending application', () => {
  const service = read('backend/src/main/java/com/demo/miniprogram/service/VolunteerService.java')
  assert.doesNotMatch(service, /报名已被拒绝，无法重新报名/)
  assert.match(service, /"CANCELLED"\.equals\(v\.getStatus\(\)\) \|\| "REJECTED"\.equals\(v\.getStatus\(\)\)/)
  assert.match(service, /\.in\(Volunteer::getStatus, "CANCELLED", "REJECTED"\)/)
  assert.match(service, /\.set\(Volunteer::getStatus, "REPLACED"\)/)
  assert.match(service, /volunteer\.setStatus\("PENDING"\)/)
})
