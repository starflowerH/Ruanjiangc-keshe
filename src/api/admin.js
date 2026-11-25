/**
 * 管理员/老师相关 API
 */

import { request } from './index'

/* ==================== 数据看板 ==================== */

/**
 * 获取系统数据统计
 * GET /api/admin/stats（管理员）
 * GET /api/teacher/stats（老师）
 */
export function getStats() {
  return request({ url: '/api/admin/stats', method: 'GET' })
}

/**
 * 获取本校数据统计（老师专用）
 * GET /api/teacher/stats
 */
export function getTeacherStats() {
  return request({ url: '/api/teacher/stats', method: 'GET' })
}

/**
 * 获取当前老师的业务权限明细（实时查库）
 * GET /api/teacher/permissions/me
 */
export function getTeacherPermissions() {
  return request({ url: '/api/teacher/permissions/me', method: 'GET' })
}

/* ==================== 志愿者审核 ==================== */

/**
 * 获取待审核志愿者列表（管理员）
 * GET /api/admin/volunteers
 */
export function getVolunteerList(params = {}) {
  return request({ url: '/api/admin/volunteers', method: 'GET', data: params })
}

/**
 * 获取本校待审核志愿者列表（老师）
 * GET /api/teacher/volunteers
 */
export function getTeacherVolunteerList(params = {}) {
  return request({ url: '/api/teacher/volunteers', method: 'GET', data: params })
}

/**
 * 审核志愿者
 * POST /api/admin/volunteers/audit
 * POST /api/teacher/volunteers/audit
 */
export function auditVolunteer(data, isTeacher = false) {
  const url = isTeacher ? '/api/teacher/volunteers/audit' : '/api/admin/volunteers/audit'
  return request({ url, method: 'POST', data })
}

/**
 * 取消志愿者资格（老师/管理员）
 * POST /api/teacher/volunteers/{id}/cancel
 * POST /api/admin/volunteers/{id}/cancel
 */
export function cancelVolunteer(id, reason = '', isTeacher = false) {
  const url = isTeacher ? `/api/teacher/volunteers/${id}/cancel` : `/api/admin/volunteers/${id}/cancel`
  return request({ url, method: 'POST', data: { reason } })
}

/**
 * 撤销取消，恢复志愿者资格（取消后2天内可撤销）
 * POST /api/teacher/volunteers/{id}/undo-cancel
 * POST /api/admin/volunteers/{id}/undo-cancel
 */
export function undoCancelVolunteer(id, isTeacher = false) {
  const url = isTeacher ? `/api/teacher/volunteers/${id}/undo-cancel` : `/api/admin/volunteers/${id}/undo-cancel`
  return request({ url, method: 'POST' })
}

/* ==================== 问答审核 ==================== */

/**
 * 获取待审核问答列表
 * GET /api/admin/qa/pending
 */
export function getPendingQA(params = {}) {
  return request({ url: '/api/admin/qa/pending', method: 'GET', data: params })
}

/**
 * 审核问答
 * POST /api/admin/qa/audit
 */
export function auditQA(data) {
  return request({ url: '/api/admin/qa/audit', method: 'POST', data })
}

/**
 * 切换问答公开状态
 * PUT /api/admin/qa/{id}/toggle-public?isPublic=1
 * 后端使用 @RequestParam 接收，必须以 query 参数形式传参（不能放 JSON body）
 */
export function toggleQAPublic(id, isPublic) {
  return request({ url: `/api/admin/qa/${id}/toggle-public?isPublic=${isPublic}`, method: 'PUT' })
}

/* ==================== 论坛治理 ==================== */

export function setForumModerator(volunteerId, active = true, isTeacher = false) {
  const url = isTeacher
    ? `/api/teacher/forum/moderators/${volunteerId}`
    : `/api/admin/forum/moderators/${volunteerId}`
  return request({ url, method: 'POST', data: { active } })
}

export function setPromotionManager(volunteerId, active = true, isTeacher = false) {
  const url = isTeacher
    ? `/api/teacher/promotion/managers/${volunteerId}`
    : `/api/admin/promotion/managers/${volunteerId}`
  return request({ url, method: 'POST', data: { active } })
}

export function getForumPendingPosts(params = {}) {
  return request({ url: '/api/admin/forum/posts', method: 'GET', data: params })
}

export function reviewForumPost(id, action) {
  return request({ url: `/api/admin/forum/posts/${id}/review`, method: 'POST', data: { action } })
}

export function setForumFeatured(id, featured) {
  return request({ url: `/api/admin/forum/posts/${id}/featured`, method: 'POST', data: { featured } })
}

export function getForumFeaturedCandidates(params = {}) {
  return request({ url: '/api/admin/forum/featured-candidates', method: 'GET', data: params })
}

export function getForumReports(params = {}) {
  return request({ url: '/api/admin/forum/reports', method: 'GET', data: params })
}

export function reviewForumReport(id, data) {
  return request({ url: `/api/admin/forum/reports/${id}/review`, method: 'POST', data })
}

export function getForumAppeals(params = {}) {
  return request({ url: '/api/admin/forum/appeals', method: 'GET', data: params })
}

export function reviewForumAppeal(id, data) {
  return request({ url: `/api/admin/forum/appeals/${id}/review`, method: 'POST', data })
}

export function getSensitiveWords() {
  return request({ url: '/api/admin/forum/sensitive-words', method: 'GET' })
}

export function addSensitiveWord(word) {
  return request({ url: '/api/admin/forum/sensitive-words', method: 'POST', data: { word } })
}

export function deleteSensitiveWord(id) {
  return request({ url: `/api/admin/forum/sensitive-words/${id}`, method: 'DELETE' })
}

/* ==================== 老师账号审核（超管） ==================== */

/**
 * 获取老师账号列表（含 status：0=待启用/已禁用，1=已启用）
 * GET /api/admin/teachers
 */
export function getTeacherList() {
  return request({ url: '/api/admin/teachers', method: 'GET' })
}

/**
 * 启用/禁用老师账号（审核老师注册）
 * POST /api/admin/teachers/{id}/enable  Body: { enabled: true/false }
 */
export function enableTeacher(id, enabled) {
  return request({ url: `/api/admin/teachers/${id}/enable`, method: 'POST', data: { enabled } })
}

/**
 * 更新老师业务权限（超级管理员）
 * PUT /api/admin/teachers/{id}/permissions
 */
export function updateTeacherPermissions(id, permissions) {
  return request({ url: `/api/admin/teachers/${id}/permissions`, method: 'PUT', data: permissions })
}

/* ==================== 队伍管理 ==================== */

/**
 * 触发自动组队
 * POST /api/admin/teams/build
 * POST /api/teacher/teams/build
 */
export function buildTeams(isTeacher = false) {
  const url = isTeacher ? '/api/teacher/teams/build' : '/api/admin/teams/build'
  return request({ url, method: 'POST' })
}

/**
 * 获取队伍列表
 * GET /api/admin/teams?highSchoolId=xxx
 */
export function getTeams(highSchoolId) {
  return request({ url: '/api/admin/teams', method: 'GET', data: { highSchoolId } })
}

/* ==================== 组队状态管理（老师端） ==================== */

/**
 * 获取本校组队状态（按高中分组）
 * GET /api/teacher/team-status
 */
export function getTeamStatus() {
  return request({ url: '/api/teacher/team-status', method: 'GET' })
}

/**
 * 一键自动组队（将本校未组队人员自动分组）
 * POST /api/teacher/teams/auto-build
 */
export function autoBuildTeams() {
  return request({ url: '/api/teacher/teams/auto-build', method: 'POST' })
}

/**
 * 老师手动创建自定义队伍
 * POST /api/teacher/teams/create
 * Body: { teamName, highSchoolId, captainVolunteerId? }
 */
export function createTeacherTeam(data) {
  return request({ url: '/api/teacher/teams/create', method: 'POST', data })
}

/**
 * 老师指定志愿者加入/移出队伍
 * POST /api/teacher/teams/{id}/members
 * Body: { volunteerId, action: 'ADD'|'REMOVE' }
 */
export function modifyTeamMember(teamId, data) {
  return request({ url: `/api/teacher/teams/${teamId}/members`, method: 'POST', data })
}

/**
 * 老师指定/更换队长
 * POST /api/teacher/teams/{id}/captain
 * Body: { volunteerId }
 */
export function assignTeamCaptain(teamId, volunteerId) {
  return request({ url: `/api/teacher/teams/${teamId}/captain`, method: 'POST', data: { volunteerId } })
}

/**
 * 老师解散队伍
 * DELETE /api/teacher/teams/{id}
 */
export function deleteTeacherTeam(teamId) {
  return request({ url: `/api/teacher/teams/${teamId}`, method: 'DELETE' })
}

/**
 * 队伍详情（老师端）
 * GET /api/teacher/teams/{id}
 */
export function getTeamDetail(teamId) {
  return request({ url: `/api/teacher/teams/${teamId}`, method: 'GET' })
}

/**
 * 修改队伍目标高中（成员目标高中同步）
 * PUT /api/teacher/teams/{id}/high-school
 */
export function updateTeamHighSchool(teamId, highSchoolId) {
  return request({ url: `/api/teacher/teams/${teamId}/high-school`, method: 'PUT', data: { highSchoolId } })
}

/**
 * 获取所有队伍列表（管理员查看全部队伍）
 * GET /api/admin/teams/all
 */
export function getAllTeams() {
  return request({ url: '/api/admin/teams/all', method: 'GET' })
}

/**
 * 分页查询所有高中生列表（管理员查看高中生信息）
 * GET /api/admin/students?page=1&size=20&keyword=xxx
 * @param {Object} params - { page, size, keyword }
 */
export function getStudentList(params) {
  return request({ url: '/api/admin/students', method: 'GET', data: params })
}

/* ==================== 队伍角色审核 ==================== */

/**
 * 获取待审核的队长申请/转让列表
 * GET /api/admin/team-applications
 */
export function getTeamApplications() {
  return request({ url: '/api/admin/team-applications', method: 'GET' })
}

/**
 * 审核队长申请/转让
 * POST /api/admin/team-applications/{id}/audit
 * @param {number} id - 申请ID
 * @param {Object} data - { action: "APPROVED" | "REJECTED" }
 */
export function auditTeamApplication(id, data) {
  return request({ url: `/api/admin/team-applications/${id}/audit`, method: 'POST', data })
}

/* ==================== 志愿者高中分配管理 ==================== */

/**
 * 获取志愿者绑定的高中列表
 * GET /api/teacher/volunteers/{id}/high-schools
 */
export function getVolunteerHighSchools(volunteerId, isTeacher = false) {
  const prefix = isTeacher ? '/api/teacher' : '/api/admin'
  return request({ url: `${prefix}/volunteers/${volunteerId}/high-schools`, method: 'GET' })
}

/**
 * 为志愿者分配/更新目标高中
 * POST /api/teacher/volunteers/{id}/high-schools
 * @param {number} volunteerId - 志愿者ID
 * @param {number[]} highSchoolIds - 高中ID列表
 */
export function assignVolunteerHighSchools(volunteerId, highSchoolIds, isTeacher = false) {
  const prefix = isTeacher ? '/api/teacher' : '/api/admin'
  return request({ url: `${prefix}/volunteers/${volunteerId}/high-schools`, method: 'POST', data: highSchoolIds })
}

/**
 * 删除志愿者的单个高中绑定
 * DELETE /api/teacher/volunteers/{id}/high-schools/{highSchoolId}
 */
export function removeVolunteerHighSchool(volunteerId, highSchoolId, isTeacher = false) {
  const prefix = isTeacher ? '/api/teacher' : '/api/admin'
  return request({ url: `${prefix}/volunteers/${volunteerId}/high-schools/${highSchoolId}`, method: 'DELETE' })
}

/**
 * 获取高中列表（公开接口）
 * GET /api/high-school/list
 */
export function getHighSchoolList() {
  return request({ url: '/api/high-school/search', method: 'GET', data: { keyword: '' } })
}
