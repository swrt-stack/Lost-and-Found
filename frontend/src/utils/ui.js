export function itemTypeTagType(type) {
  if (type === 'found' || type === 'FOUND') return 'success'
  if (type === 'lost' || type === 'LOST') return 'warning'
  return 'info'
}

export function itemTypeLabel(type) {
  if (type === 'found' || type === 'FOUND') return '招领物品'
  if (type === 'lost' || type === 'LOST') return '遗失物品'
  return type || '-'
}

export function statusTagType(status) {
  if (status === 'APPROVED' || status === 'ACTIVE' || status === 'RESOLVED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED' || status === 'DISABLED') return 'danger'
  if (status === 'COMPLETED') return 'primary'
  if (status === 'OFFLINE' || status === 'DRAFT') return 'info'
  return ''
}

export function statusLabel(status) {
  if (status === 'APPROVED') return '已通过'
  if (status === 'ACTIVE') return '正常'
  if (status === 'RESOLVED') return '已处理'
  if (status === 'PENDING') return '待审核'
  if (status === 'REJECTED') return '已驳回'
  if (status === 'DISABLED') return '已禁用'
  if (status === 'COMPLETED') return '已完成'
  if (status === 'OFFLINE') return '已下架'
  if (status === 'DRAFT') return '草稿'
  return status || '-'
}

export function roleTagType(role) {
  if (role === 'SYS_ADMIN') return 'danger'
  if (role === 'REVIEW_ADMIN') return 'warning'
  if (role === 'USER') return 'success'
  return 'info'
}

export function roleLabel(role) {
  if (role === 'SYS_ADMIN') return '超级管理员'
  if (role === 'REVIEW_ADMIN') return '审核管理员'
  if (role === 'USER') return '普通用户'
  return role || '-'
}

export function statusClass(status) {
  const type = statusTagType(status)
  if (type === 'success' || type === 'primary') return 'success'
  if (type === 'warning') return 'pending'
  if (type === 'danger') return 'danger'
  return 'pending'
}

export function formatItemImage(images) {
  if (!images) return ''
  return String(images)
    .split(',')
    .map((item) => item.trim())
    .find(Boolean) || ''
}

export function safeAlert(message) {
  window.alert(message)
}

export function safeConfirm(message) {
  return window.confirm(message)
}

export async function runConfirmedAction({ confirmMessage, action, successMessage, errorMessage }) {
  if (confirmMessage && !safeConfirm(confirmMessage)) {
    return false
  }
  try {
    await action()
    if (successMessage) {
      safeAlert(successMessage)
    }
    return true
  } catch (error) {
    const detail = error?.message || '操作失败'
    safeAlert(errorMessage ? `${errorMessage}：${detail}` : detail)
    return false
  }
}
