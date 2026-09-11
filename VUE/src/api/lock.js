import api from './http'

export const signUp = (data) => api.post('/api/auth/sign-up', data).then((r) => r.data)
export const signIn = (data) => api.post('/api/auth/sign-in', data).then((r) => r.data)
export const getProfile = () => api.get('/api/me/profile').then((r) => r.data)
export const updateProfile = (data) => api.put('/api/me/profile', data).then((r) => r.data)

export const createLock = (data) => api.post('/api/locks', data).then((r) => r.data)
export const getByWearer = (token) => api.get(`/api/locks/by-wearer/${token}`).then((r) => r.data)
export const getByKeyholder = (token) =>
  api.get(`/api/locks/by-keyholder/${token}`).then((r) => r.data)
export const listEvents = (token, role) =>
  api.get(`/api/locks/${token}/events`, { params: { role } }).then((r) => r.data)
export const unlock = (data) => api.post('/api/locks/unlock', data).then((r) => r.data)
export const hygieneStart = (data) => api.post('/api/locks/hygiene/start', data).then((r) => r.data)
export const hygieneEnd = (data) => api.post('/api/locks/hygiene/end', data).then((r) => r.data)
export const addTime = (data) => api.post('/api/locks/keyholder/add-time', data).then((r) => r.data)
export const subTime = (data) => api.post('/api/locks/keyholder/sub-time', data).then((r) => r.data)
export const setFreeze = (data) => api.post('/api/locks/keyholder/freeze', data).then((r) => r.data)
export const setMinLock = (data) =>
  api.post('/api/locks/keyholder/min-lock', data).then((r) => r.data)
export const setEndPhrase = (data) =>
  api.post('/api/locks/keyholder/end-phrase', data).then((r) => r.data)
export const setObedience = (data) =>
  api.post('/api/locks/keyholder/obedience', data).then((r) => r.data)
export const pollObedience = (token) =>
  api.post('/api/locks/obedience/poll', { token }).then((r) => r.data)
export const completeObedience = (data) =>
  api.post('/api/locks/obedience/complete', data).then((r) => r.data)
export const photoRequest = (data) =>
  api.post('/api/locks/keyholder/photo-request', data).then((r) => r.data)
export const photoSubmit = (data) => api.post('/api/locks/wearer/photo', data).then((r) => r.data)
export const createTask = (data) => api.post('/api/locks/tasks', data).then((r) => r.data)
export const listTasks = (token, role) =>
  api.get('/api/locks/tasks', { params: { token, role } }).then((r) => r.data)
export const completeTask = (data) =>
  api.post('/api/locks/tasks/complete', data).then((r) => r.data)
export const integritySync = (data) =>
  api.post('/api/locks/integrity/sync', data).then((r) => r.data)
export const claimKeyholder = (token) =>
  api.post('/api/locks/claim-keyholder', { token }).then((r) => r.data)
export const myKeyholderLocks = () => api.get('/api/me/locks/keyholder').then((r) => r.data)
