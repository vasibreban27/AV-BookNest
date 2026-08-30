export const CONTACT_TOPICS = [
  'GENERAL',
  'ORDER',
  'PAYMENT',
  'DELIVERY',
  'ACCOUNT',
  'LISTING',
  'PRIVACY',
  'OTHER',
] as const

export type ContactTopic = (typeof CONTACT_TOPICS)[number]

export type ContactPayload = {
  name: string
  email: string
  topic: ContactTopic
  subject: string
  message: string
  privacyAccepted: boolean
  website: string
  orderId?: number
  bookId?: number
}

export type ContactResponse = {
  message: string
  ticketId: number | null
  reference: string | null
}
