import { api } from '../../../api/client'
import type {
  AuthResponse,
  LoginPayload,
  MessageResponse,
  RegisterPayload,
  UpdateProfilePayload,
  ChangePasswordPayload,
  User,
} from '../types/auth.types'

export const authApi = {
  async login(payload: LoginPayload) {
    const { data } = await api.post<AuthResponse>('/auth/login', payload)
    return data
  },

  async register(payload: RegisterPayload) {
    const { data } = await api.post<MessageResponse>('/auth/register', payload)
    return data
  },

  async resendVerification(email: string) {
    const { data } = await api.post<MessageResponse>('/auth/verification-email', { email })
    return data
  },

  async verifyEmail(token: string) {
    const { data } = await api.post<MessageResponse>('/auth/verify-email', { token })
    return data
  },

  async forgotPassword(email: string) {
    const { data } = await api.post<MessageResponse>('/auth/forgot-password', { email })
    return data
  },

  async resetPassword(token: string, password: string) {
    const { data } = await api.post<MessageResponse>('/auth/reset-password', { token, password })
    return data
  },

  async currentUser() {
    const { data } = await api.get<User>('/auth/me')
    return data
  },

  async updateProfile(payload: UpdateProfilePayload) {
    const { data } = await api.patch<User>('/auth/me/profile', payload)
    return data
  },

  async changePassword(payload: ChangePasswordPayload) {
    const { data } = await api.patch<MessageResponse>('/auth/me/password', payload)
    return data
  },

  async logout(refreshToken: string) {
    await api.post('/auth/logout', { refreshToken })
  },
}
