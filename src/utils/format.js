/**
 * 格式化时间为相对时间（如：5分钟前、2小时前、3天前）
 * @param {string|Date} time - 时间字符串或Date对象
 * @returns {string} 相对时间
 */
export function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 0) return '刚刚'
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  if (diff < 2592000000) return Math.floor(diff / 604800000) + '周前'

  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}月${day}日`
}

/**
 * 在昵称后追加身份标签：student → -高中生，其余（volunteer 等）→ -大学生
 * @param {string} name 昵称/姓名
 * @param {string} type 身份类型（student/volunteer 等）
 * @returns {string} 带标签的名称
 */
export function formatIdentityName(name, type) {
  const suffix = type === 'student' ? '-高中生' : '-大学生'
  return (name || '匿名用户') + suffix
}
