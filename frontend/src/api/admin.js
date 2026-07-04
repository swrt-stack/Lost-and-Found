import client from './client'

export function getDashboard() {
  return client.get('/admin/dashboard')
}

export function getSpatiotemporalAnalysis(topK = 5) {
  return client.get('/admin/spatiotemporal/analysis', { params: { topK }, timeout: 60000 })
}

export function getAdminLostItems(keyword) {
  return client.get('/admin/lost-items', { params: { keyword } })
}

export function getAdminFoundItems(keyword) {
  return client.get('/admin/found-items', { params: { keyword } })
}

export function getAdminItemDetail(id) {
  return client.get(`/admin/items/${id}`)
}

export function getReviews() {
  return client.get('/admin/reviews')
}

export function getReviewHistory() {
  return client.get('/admin/review-history')
}

export function getAdminClaims() {
  return client.get('/admin/claims')
}

export function approveAdminClaim(id, remark = '') {
  return client.post(`/admin/claims/${id}/approve`, { remark })
}

export function rejectAdminClaim(id, remark = '') {
  return client.post(`/admin/claims/${id}/reject`, { remark })
}

export function approveReview(id, remark = '') {
  return client.post(`/admin/reviews/${id}/approve`, { remark })
}

export function rejectReview(id, remark = '') {
  return client.post(`/admin/reviews/${id}/reject`, { remark })
}

export function deleteReview(id, remark = '') {
  return client.post(`/admin/reviews/${id}/delete`, { remark })
}

export function getAdminAnnouncements() {
  return client.get('/admin/announcements')
}

export function createAnnouncement(data) {
  return client.post('/admin/announcements', data)
}

export function updateAnnouncement(id, data) {
  return client.put(`/admin/announcements/${id}`, data)
}

export function deleteAnnouncement(id) {
  return client.delete(`/admin/announcements/${id}`)
}

export function getReports() {
  return client.get('/admin/reports')
}

export function resolveReport(id, remark = '') {
  return client.post(`/admin/reports/${id}/resolve`, { remark })
}

export function rejectReport(id, remark = '') {
  return client.post(`/admin/reports/${id}/reject`, { remark })
}
