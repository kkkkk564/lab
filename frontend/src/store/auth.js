import { defineStore } from 'pinia'
import api from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('lab_token') || '',
    user: JSON.parse(localStorage.getItem('lab_user') || 'null')
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => !!s.user && s.user.role === 'ADMIN',
    roleText: (s) => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[(s.user && s.user.role) || ''] || '未知')
  },
  actions: {
    async login(form) {
      const data = await api.login(form)
      this.token = data.token
      this.user = data.user
      localStorage.setItem('lab_token', data.token)
      localStorage.setItem('lab_user', JSON.stringify(data.user))
      return data.user
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('lab_token')
      localStorage.removeItem('lab_user')
    }
  }
})
