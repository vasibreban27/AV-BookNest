import { api } from '../../../api/client'
import type {
  AuthResponse,
  LoginPayload,
  RegisterPayload,
  User,
} from '../types/auth.types'

export const authApi = {
  async login(payload: LoginPayload) {
    const { data } = await api.post<AuthResponse>('/auth/login', payload)
    return data
  },

  async register(payload: RegisterPayload) {
    const { data } = await api.post<AuthResponse>('/auth/register', payload)
    return data
  },

  async currentUser() {
    const { data } = await api.get<User>('/auth/me')
    return data
  },

  async logout(refreshToken: string) {
    await api.post('/auth/logout', { refreshToken })
  },
}
