import client from './client'

export function getCategories() {
  return client.get('/categories')
}

export function createCategory(data) {
  return client.post('/categories', data)
}

export function deleteCategory(id) {
  return client.delete(`/categories/${id}`)
}
