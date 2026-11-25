import { request } from './index'

export function reportPost(data) {
  // data: { postId, targetType: 'post'|'comment', commentId?, reason, detail }
  return request({ url: '/api/forum/reports', method: 'POST', data })
}

export function getMyPenalties() {
  return request({ url: '/api/forum/penalties/mine', method: 'GET' })
}

export function submitAppeal(data) {
  return request({ url: '/api/forum/appeals', method: 'POST', data })
}

export function getModeratorStatus() {
  return request({ url: '/api/forum/moderation/status', method: 'GET' })
}

export function getPendingPosts(params = {}) {
  return request({ url: '/api/forum/moderation/posts', method: 'GET', data: params })
}

export function reviewPost(id, action) {
  return request({ url: `/api/forum/moderation/posts/${id}/review`, method: 'POST', data: { action } })
}

export function setFeatured(id, featured) {
  return request({ url: `/api/forum/moderation/posts/${id}/featured`, method: 'POST', data: { featured } })
}

export function getFeaturedCandidates(params = {}) {
  return request({ url: '/api/forum/moderation/featured-candidates', method: 'GET', data: params })
}

export function getPendingReports(params = {}) {
  return request({ url: '/api/forum/moderation/reports', method: 'GET', data: params })
}

export function reviewReport(id, data) {
  return request({ url: `/api/forum/moderation/reports/${id}/review`, method: 'POST', data })
}

export function getPendingAppeals(params = {}) {
  return request({ url: '/api/forum/moderation/appeals', method: 'GET', data: params })
}

export function reviewAppeal(id, data) {
  return request({ url: `/api/forum/moderation/appeals/${id}/review`, method: 'POST', data })
}
