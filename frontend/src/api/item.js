import client from './client'

export function searchItems(params) {
  return client.get('/items/search', { params })
}

export function publishLost(data) {
  return client.post('/items/lost', data)
}

export function publishFound(data) {
  return client.post('/items/found', data)
}

export function getMyItems() {
  return client.get('/items/mine')
}

export function updateItem(itemId, data) {
  return client.put(`/items/${itemId}`, data)
}

export function offlineItem(itemId) {
  return client.post(`/items/${itemId}/offline`)
}

export function deleteItem(itemId) {
  return client.delete(`/items/${itemId}`)
}

export function reportItem(itemId, reason) {
  return client.post(`/items/${itemId}/report`, { reason })
}

export function getMyClaims() {
  return client.get('/items/claims')
}

export function claimFoundItem(itemId, message) {
  return client.post(`/items/${itemId}/claim`, { message })
}

export function approveClaim(claimId, remark) {
  return client.post(`/items/claims/${claimId}/approve`, { remark })
}

export function rejectClaim(claimId, remark) {
  return client.post(`/items/claims/${claimId}/reject`, { remark })
}

export function completeItem(itemId) {
  return client.post(`/items/${itemId}/complete`)
}

export function getItemChatContacts(itemId) {
  return client.get(`/items/${itemId}/chats/contacts`)
}

export function getItemChatMessages(itemId, counterpartUserId) {
  return client.get(`/items/${itemId}/chats/messages`, {
    params: counterpartUserId ? { counterpartUserId } : {},
  })
}

export function sendItemChatMessage(itemId, data) {
  return client.post(`/items/${itemId}/chats/messages`, data)
}
