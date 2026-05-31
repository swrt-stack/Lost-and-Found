import client from './client'

export function getSystemConfig() {
  return client.get('/config/system')
}

export function updateSystemConfig(data) {
  return client.put('/config/system', data)
}
