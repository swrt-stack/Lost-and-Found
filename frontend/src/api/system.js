import client from './client'

export function getSystemDict() {
  return client.get('/system/dict')
}

export function getOverview() {
  return client.get('/system/overview')
}

export function getAnnouncements() {
  return client.get('/system/announcements')
}

export function getLogs() {
  return client.get('/logs')
}

export function getProfile() {
  return client.get('/user/profile')
}

export function updateProfile(data) {
  return client.put('/user/profile', data)
}

export function getUsers() {
  return client.get('/user/list')
}

export function updateUserStatus(id, status) {
  return client.patch(`/user/${id}/status`, { status })
}

export function updateUserRole(id, role) {
  return client.patch(`/user/${id}/role`, { role })
}
