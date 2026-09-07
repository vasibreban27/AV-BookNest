import type { Book, BookStatus } from '../../catalog/types/catalog.types'
import type {
  Order,
  OrderIssueStatus,
  OrderStatus,
  SellerOrderStatus,
  Shipment,
} from '../../orders/types/orders.types'

export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  hasNext: boolean
}

export type BookModerationStatus = 'VISIBLE' | 'HIDDEN'
export type BookModerationReason =
  | 'POLICY_VIOLATION'
  | 'SPAM'
  | 'FRAUD_SUSPECTED'
  | 'ACCOUNT_SUSPENDED'
  | 'OTHER'
export type IssueResolution = 'REFUND_BUYER' | 'RELEASE_SELLER_PAYOUT'
export type IntegrationEventStatus = 'PENDING' | 'PROCESSING' | 'PROCESSED' | 'FAILED'
export type SellerTransferStatus = 'BLOCKED' | 'READY' | 'CREATED' | 'PAID' | 'FAILED' | 'REVERSED'

export type AdminDashboard = {
  newUsersLast7Days: number
  activeListings: number
  paidOrders: number
  openIssues: number
  shipmentExceptions: number
  failedTransfers: number
  failedIntegrations: number
  netGmv: number
  activeCommission: number
}

export type AdminUser = {
  id: number
  firstName: string
  lastName: string
  email: string
  phoneNumber: string | null
  role: 'USER' | 'ADMIN'
  enabled: boolean
  emailVerified: boolean
  stripePayoutsEnabled: boolean
  suspendedAt: string | null
  suspendedUntil?: string | null
  suspensionReason: string | null
  createdAt: string
}

export type AdminUserDetails = {
  user: AdminUser
  listingCount: number
  buyerOrderCount: number
  sellerOrderCount: number
}

export type AdminBook = {
  book: Book
  moderationStatus: BookModerationStatus
  moderationReason: BookModerationReason | null
  moderationNote: string | null
  moderatedById: number | null
  moderatedAt: string | null
}

export type AdminCategory = {
  id: number
  name: string
  slug: string
  description: string | null
  active: boolean
  createdAt: string
}

export type AdminOrderSummary = {
  id: number
  orderNumber: string
  buyerId: number
  buyerEmail: string
  status: OrderStatus
  totalAmount: number
  currency: string
  placedAt: string
}

export type AdminOrderDetails = {
  order: Order
  buyerId: number
  buyerEmail: string
  recipientPhone: string
}

export type AdminIssue = {
  sellerOrderId: number
  orderId: number
  orderNumber: string
  buyerId: number
  buyerEmail: string
  sellerId: number
  sellerEmail: string
  sellerOrderStatus: SellerOrderStatus
  itemSubtotal: number
  sellerProceeds: number
  issueStatus: OrderIssueStatus
  issueReason: string | null
  issueOpenedAt: string | null
  issueResolvedAt: string | null
  resolution: IssueResolution | null
  resolutionNote: string | null
  resolvedById: number | null
}

export type AdminIntegrationEvent = {
  id: number
  aggregateType: string
  aggregateId: number
  eventType: string
  status: IntegrationEventStatus
  attempts: number
  nextAttemptAt: string
  lastError: string | null
  createdAt: string
}

export type AdminTransfer = {
  id: number
  sellerOrderId: number
  amount: number
  currency: string
  status: SellerTransferStatus
  providerTransferId: string | null
  failureReason: string | null
  updatedAt: string
}

export type AdminShipmentException = {
  id: number
  sellerOrderId: number
  shipment: Shipment
  updatedAt: string
}

export type AdminAuditLog = {
  id: number
  administratorId: number
  administratorEmail: string
  action: string
  targetType: string
  targetId: number | null
  reason: string | null
  details: string | null
  createdAt: string
}

export type AdminPageRequest = { page?: number; size?: number }
export type AdminUserFilters = AdminPageRequest & { query?: string; enabled?: boolean }
export type AdminBookFilters = AdminPageRequest & {
  query?: string
  status?: BookStatus
  moderationStatus?: BookModerationStatus
}
export type AdminOrderFilters = AdminPageRequest & { query?: string; status?: OrderStatus }
export type AdminIssueFilters = AdminPageRequest & { status?: OrderIssueStatus }

export type CategoryPayload = { name: string; slug: string; description: string }
export type CategoryUpdatePayload = Pick<CategoryPayload, 'name' | 'description'>
export type BookModerationPayload = { reason: BookModerationReason; note: string }
export type IssueResolutionPayload = { resolution: IssueResolution; note: string }
