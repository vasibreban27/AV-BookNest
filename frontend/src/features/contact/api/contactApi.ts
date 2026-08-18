import { api } from '../../../api/client'
import type { ContactPayload, ContactResponse } from '../types/contact.types'

export const contactApi = {
  async send(payload: ContactPayload) {
    const response = await api.post<ContactResponse>('/contact', payload)
    return response.data
  },
}
