import client from './client'

export function getAiGatewayStatus() {
  return client.get('/ai/status')
}

export function smartMatch(data) {
  const formData = new FormData()
  if (data.file) {
    formData.append('file', data.file)
  }
  if (data.description) {
    formData.append('description', data.description)
  }
  formData.append('topK', String(data.topK || 10))
  return client.post('/ai/smart-match', formData, { timeout: 120000 })
}
