import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('lab_token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        ElMessage.error(body.msg || '请求失败')
        return Promise.reject(new Error(body.msg))
      }
      return body.data
    }
    return body
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('lab_token')
      localStorage.removeItem('lab_user')
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    } else {
      const msg = err.response && err.response.data && err.response.data.msg
      ElMessage.error(msg || '网络异常，请稍后重试')
    }
    return Promise.reject(err)
  }
)

export const api = {
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  changePassword: (data) => http.put('/auth/password', data),

  labs: (params) => http.get('/labs', { params }),
  labDetail: (id) => http.get(`/labs/${id}`),
  labTimetable: (id, date) => http.get(`/labs/${id}/timetable`, { params: { date } }),
  createLab: (data) => http.post('/labs', data),
  updateLab: (id, data) => http.put(`/labs/${id}`, data),
  updateLabStatus: (id, status) => http.put(`/labs/${id}/status`, null, { params: { status } }),
  deleteLab: (id) => http.delete(`/labs/${id}`),

  devices: (params) => http.get('/devices', { params }),
  createDevice: (data) => http.post('/devices', data),
  updateDevice: (id, data) => http.put(`/devices/${id}`, data),
  deleteDevice: (id) => http.delete(`/devices/${id}`),
  reportDevice: (id, data) => http.post(`/devices/${id}/report`, data),
  repairDevice: (id) => http.put(`/devices/${id}/repair`),

  reservations: (params) => http.get('/reservations', { params }),
  createReservation: (data) => http.post('/reservations', data),
  cancelReservation: (id) => http.put(`/reservations/${id}/cancel`),
  reviewReservation: (id, data) => http.put(`/reservations/${id}/review`, data),

  announcements: () => http.get('/announcements'),
  createAnnouncement: (data) => http.post('/announcements', data),
  updateAnnouncement: (id, data) => http.put(`/announcements/${id}`, data),
  deleteAnnouncement: (id) => http.delete(`/announcements/${id}`),

  statsOverview: () => http.get('/stats/overview'),
  statsTrends: (days = 14) => http.get('/stats/trends', { params: { days } }),
  statsLabUsage: (days = 14) => http.get('/stats/lab-usage', { params: { days } }),
  statsStatusDistribution: () => http.get('/stats/status-distribution'),

  users: (params) => http.get('/users', { params }),
  createUser: (data) => http.post('/users', data),
  updateUser: (id, data) => http.put(`/users/${id}`, data),
  deleteUser: (id) => http.delete(`/users/${id}`),
  resetUserPassword: (id) => http.put(`/users/${id}/reset-password`)
}

export default api
