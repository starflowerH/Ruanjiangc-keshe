/**
 * 评论相关 API
 * 对应后端：ForumCommentController (/api/comment)
 */

import { request } from './index'

/**
 * 添加评论
 * POST /api/comment/add
 */
export function addComment(data) {
  return request({ url: '/api/comment/add', method: 'POST', data })
}

/**
 * 获取帖子的评论列表
 * GET /api/comment/list?postId=1
 */
export function getComments(postId) {
  return request({ url: '/api/comment/list', method: 'GET', data: { postId } })
}

/**
 * 获取我的评论列表
 * GET /api/comment/my-comments?page=1&size=10
 */
export function getMyComments(params = {}) {
  return request({ url: '/api/comment/my-comments', method: 'GET', data: params })
}

/**
 * 评论点赞切换
 * POST /api/comment/{id}/like
 */
export function toggleCommentLike(commentId) {
  return request({ url: `/api/comment/${commentId}/like`, method: 'POST' })
}
