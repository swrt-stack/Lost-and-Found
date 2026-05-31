import client from './client'

export function getChatConversations() {
  return client.get('/chats/conversations')
}

export function getChatSummary() {
  return client.get('/chats/summary')
}
