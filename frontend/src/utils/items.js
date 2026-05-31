import { formatItemImage, itemTypeLabel, statusClass, statusLabel } from './ui'

export function normalizeItem(item = {}) {
  return {
    id: item.id || item.itemId || '',
    title: item.title || '-',
    type: item.type || '',
    typeLabel: itemTypeLabel(item.type),
    category: item.category || '-',
    location: item.location || '-',
    time: item.time || '-',
    publisher: item.publisher || '-',
    publisherAvatarUrl: item.publisherAvatarUrl || '',
    status: item.status || '',
    statusLabel: statusLabel(item.status),
    statusClass: statusClass(item.status),
    description: item.description || '暂无描述',
    contact: item.contact || '',
    pickupMethod: item.pickupMethod || '',
    images: item.images || '',
    imageUrl: formatItemImage(item.images),
  }
}

export function normalizeReviewItem(item = {}) {
  return {
    id: item.id || '',
    title: item.title || '-',
    type: item.type || '',
    typeLabel: itemTypeLabel(item.type),
    category: item.category || '-',
    publisher: item.publisher || '-',
    publisherAvatarUrl: item.publisherAvatarUrl || '',
    status: item.status || '',
    statusLabel: statusLabel(item.status),
    statusClass: statusClass(item.status),
    description: item.description || '暂无描述',
    location: item.location || '-',
    time: item.time || '-',
  }
}

export function toDateTimeInputValue(value) {
  if (!value || value === '-') return ''
  return value.replace(' ', 'T').slice(0, 16)
}

export function toBackendDateTime(value) {
  if (!value) return ''
  return `${value.replace('T', ' ')}:00`
}
