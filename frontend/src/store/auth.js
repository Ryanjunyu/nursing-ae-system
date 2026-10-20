// 简单的登录态本地存储工具（token + 当前用户）
export const auth = {
  getToken() {
    return localStorage.getItem('ae_token')
  },
  setToken(t) {
    localStorage.setItem('ae_token', t)
  },
  getUser() {
    try {
      return JSON.parse(localStorage.getItem('ae_user'))
    } catch {
      return null
    }
  },
  setUser(u) {
    localStorage.setItem('ae_user', JSON.stringify(u))
  },
  clear() {
    localStorage.removeItem('ae_token')
    localStorage.removeItem('ae_user')
  }
}
