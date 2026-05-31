import client from './client'

export function getMessages() {
  return client.get('/messages')
}

export function getMessageSummary() {
  return client.get('/messages/summary')
}

export function markMessageRead(id) {
  return client.post(`/messages/${id}/read`)
}

export function markAllMessagesRead() {
  return client.post('/messages/read-all')
}
