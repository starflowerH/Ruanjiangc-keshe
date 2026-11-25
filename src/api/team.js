/**
 * 队伍相关 API
 * 对应后端：TeamController (/api/team)
 */

import { request } from './index'

/* ==================== 自由组队 ==================== */

/**
 * 创建队伍
 * POST /api/team/create
 * @param {Object} data - { teamName, captainContactPhone, captainContactWechat }
 */
export function createTeam(data) {
  return request({ url: '/api/team/create', method: 'POST', data })
}

/**
 * 通过队伍号加入队伍
 * POST /api/team/join
 * @param {Object} data - { teamCode }
 */
export function joinTeam(data) {
  return request({ url: '/api/team/join', method: 'POST', data })
}

/**
 * 退出队伍
 * POST /api/team/leave
 */
export function leaveTeam() {
  return request({ url: '/api/team/leave', method: 'POST' })
}

/**
 * 获取我的队伍信息
 * GET /api/team/my
 */
export function getMyTeam() {
  return request({ url: '/api/team/my', method: 'GET' })
}

/**
 * 获取队伍成员列表
 * GET /api/team/{id}/members
 * @param {number} teamId - 队伍ID
 */
export function getTeamMembers(teamId) {
  return request({ url: `/api/team/${teamId}/members`, method: 'GET' })
}

/**
 * 更新队伍简介
 * POST /api/team/update-desc
 * @param {Object} data - { teamId, description }
 */
export function updateTeamDescription(data) {
  return request({ url: '/api/team/update-desc', method: 'POST', data })
}

/** 队长修改队伍目标高中 */
export function updateTeamTarget(data) {
  return request({ url: '/api/team/update-target', method: 'POST', data })
}

/** 队长维护对外通讯方式 */
export function updateCaptainContact(data) {
  return request({ url: '/api/team/update-contact', method: 'POST', data })
}

/**
 * 获取队伍成员详情（点击头像查看，仅同队可见）
 * GET /api/team/member/{volunteerId}
 * @param {number} volunteerId - 志愿者ID
 */
export function getMemberDetail(volunteerId) {
  return request({ url: `/api/team/member/${volunteerId}`, method: 'GET' })
}

/* ==================== 队长申请/转让 ==================== */

/**
 * 申请成为队长或转让队长
 * POST /api/team/apply-role
 * @param {Object} data - { teamId, applyRole, reason }
 */
export function applyForRole(data) {
  return request({ url: '/api/team/apply-role', method: 'POST', data })
}

/**
 * 队长转让队长身份
 * POST /api/team/transfer-captain
 * @param {Object} data - { teamId, targetVolunteerId, reason }
 */
export function transferCaptain(data) {
  return request({ url: '/api/team/transfer-captain', method: 'POST', data })
}

/**
 * 获取我的申请记录
 * GET /api/team/my-applications
 */
export function getMyApplications() {
  return request({ url: '/api/team/my-applications', method: 'GET' })
}

/* ==================== 兼容旧接口 ==================== */

/**
 * 获取所有队伍列表（按高中分组）
 * GET /api/admin/teams
 */
export function getAllTeams(params = {}) {
  return request({ url: '/api/admin/teams', method: 'GET', data: params })
}

/**
 * 根据目标高中获取队伍列表
 * GET /api/volunteer/by-high-school?highSchoolId=xxx
 * @param {number} highSchoolId - 高中 ID
 */
export function getTeamsByHighSchool(highSchoolId) {
  return request({ url: '/api/volunteer/by-high-school', method: 'GET', data: { highSchoolId } })
}

/**
 * 获取队长通讯录（其他队长列表，仅姓名+学校）
 * GET /api/volunteer/captain-contacts
 */
export function getCaptainContacts() {
  return request({ url: '/api/volunteer/captain-contacts', method: 'GET' })
}

/**
 * 获取本队队员列表（队长可查看）
 * GET /api/volunteer/my-team-members
 */
export function getMyTeamMembers() {
  return request({ url: '/api/volunteer/my-team-members', method: 'GET' })
}
