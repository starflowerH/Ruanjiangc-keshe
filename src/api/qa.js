/**
 * 问答相关 API
 * 对应后端：QAController (/api/qa)
 */

import { request } from './index'

/**
 * 高中生提交问题
 * POST /api/qa/ask
 */
export function submitQuestion(data) {
  return request({ url: '/api/qa/ask', method: 'POST', data })
}

/**
 * 获取我提出的问题列表（高中生）
 * GET /api/qa/my-questions
 */
export function getMyQuestions(params = {}) {
  return request({ url: '/api/qa/my-questions', method: 'GET', data: params })
}

/**
 * 获取我收到的问题列表（大学生）
 * GET /api/qa/to-me
 */
export function getReceivedQuestions(params = {}) {
  return request({ url: '/api/qa/to-me', method: 'GET', data: params })
}

/**
 * 获取我已回答的问题列表（志愿者）
 * GET /api/qa/answered-by-me
 */
export function getMyPosts(params = {}) {
  return request({ url: '/api/qa/my-posts', method: 'GET', data: params })
}

/**
 * 回答问题（大学生）
 * POST /api/qa/answer
 */
export function answerQuestion(data) {
  return request({ url: '/api/qa/answer', method: 'POST', data })
}

/**
 * 获取精选问答列表
 * GET /api/qa/featured
 */
export function getFeaturedQA(params = {}) {
  return request({ url: '/api/qa/featured', method: 'GET', data: params })
}

/**
 * 获取问答详情
 * @param {number} id - 问答ID
 */
export function getQADetail(id) {
  return request({ url: `/api/qa/${id}`, method: 'GET' })
}

/**
 * 获取热门问答（今日热度最高）
 * GET /api/qa/hot?limit=10
 */
export function getHotQuestions(limit = 10) {
  return request({ url: '/api/qa/hot', method: 'GET', data: { limit } })
}

/**
 * 获取论坛帖子列表
 * GET /api/qa/forum?page=1&size=20
 */
export function getForumPosts(params = {}) {
  return request({ url: '/api/qa/forum', method: 'GET', data: params })
}

/**
 * 帖子点赞切换（已赞取消，未赞点赞）
 * POST /api/qa/{id}/like
 */
export function toggleLike(qaId) {
  return request({ url: `/api/qa/${qaId}/like`, method: 'POST' })
}

/**
 * 回答点赞切换
 * POST /api/qa/{id}/answer-like
 */
export function toggleAnswerLike(qaId) {
  return request({ url: `/api/qa/${qaId}/answer-like`, method: 'POST' })
}

/**
 * 收藏切换（已收藏取消，未收藏收藏）
 * POST /api/qa/{id}/favorite
 */
export function toggleFavorite(qaId) {
  return request({ url: `/api/qa/${qaId}/favorite`, method: 'POST' })
}

/**
 * 我的收藏列表（分页）
 * GET /api/qa/my-favorites?page=1&size=20
 */
export function getMyFavorites(params = {}) {
  return request({ url: '/api/qa/my-favorites', method: 'GET', data: params })
}
