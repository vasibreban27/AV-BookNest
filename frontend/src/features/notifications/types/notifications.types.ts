export type NotificationType =
  | 'ORDER_PLACED'
  | 'BOOK_RESERVED'
  | 'SHIPMENT_ACCEPTED'
  | 'AWB_CREATED'
  | 'SHIPMENT_IN_TRANSIT'
  | 'SHIPMENT_DELIVERED'
  | 'ORDER_CANCELLED'
  | 'PAYMENT_SUCCEEDED'
  | 'PAYMENT_REFUNDED'
  | 'SELLER_TRANSFER_CREATED'
  | 'SELLER_ACTION_REQUIRED'
  | 'SHIPMENT_PROBLEM'
  | 'ORDER_ISSUE_OPENED'
  | 'ORDER_ISSUE_RESOLVED'
  | 'SUPPORT_REPLY'
  | 'SUPPORT_STATUS_CHANGED'
  | 'SUPPORT_ASSIGNED'
  | 'REPORT_RESOLVED'
  | 'MODERATION_NOTICE'

export type Notification = {
  id: number
  type: NotificationType
  title: string
  message: string
  actionUrl: string | null
  readAt: string | null
  createdAt: string
}
