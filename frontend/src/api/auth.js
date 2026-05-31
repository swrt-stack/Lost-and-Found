import client from './client'

export function getCaptcha() {
  return client.get('/auth/captcha')
}

export function login(data) {
  return client.post('/auth/login', data)
}

export function register(data) {
  return client.post('/auth/register', data)
}
