import type {
  OrderStatus,
  PaymentProvider,
  PaymentStatus,
  SellerOrderStatus,
  ShipmentStatus,
} from '../types/orders.types'

const orderStatusLabels: Record<OrderStatus, string> = {
  PENDING: 'În așteptare',
  PAID: 'Plătită',
  PROCESSING: 'În pregătire',
  SHIPPED: 'Expediată',
  DELIVERED: 'Livrată',
  CANCELLED: 'Anulată',
  REFUNDED: 'Rambursată',
}

const paymentStatusLabels: Record<PaymentStatus, string> = {
  PENDING: 'Așteaptă plata Stripe',
  SUCCEEDED: 'Achitată',
  FAILED: 'Plată eșuată',
  CANCELLED: 'Plată anulată',
  PARTIALLY_REFUNDED: 'Rambursată parțial',
  REFUNDED: 'Rambursată',
}

const sellerOrderStatusLabels: Record<SellerOrderStatus, string> = {
  AWAITING_SELLER: 'Așteaptă acceptarea',
  ACCEPTED: 'Acceptată',
  FULFILLED: 'Finalizată',
  CANCELLED: 'Anulată',
}

const shipmentStatusLabels: Record<ShipmentStatus, string> = {
  NOT_CREATED: 'Așteaptă acceptarea',
  AWB_PENDING: 'Se generează AWB-ul',
  AWB_CREATED: 'AWB creat',
  AWAITING_DROPOFF: 'Așteaptă predarea',
  IN_TRANSIT: 'În tranzit',
  DELIVERED: 'Livrată',
  RETURNED: 'Returnată',
  LOST: 'Problemă la transport',
  CANCELLED: 'Anulată',
}

const paymentProviderLabels: Record<PaymentProvider, string> = {
  STRIPE: 'Card online prin Stripe',
}

export function formatOrderPrice(amount: number, currency = 'RON') {
  return new Intl.NumberFormat('ro-RO', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatOrderDate(value: string) {
  return new Intl.DateTimeFormat('ro-RO', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export function formatOrderStatus(status: OrderStatus) {
  return orderStatusLabels[status]
}

export function formatPaymentStatus(status: PaymentStatus) {
  return paymentStatusLabels[status]
}

export function formatSellerOrderStatus(status: SellerOrderStatus) {
  return sellerOrderStatusLabels[status]
}

export function formatShipmentStatus(status: ShipmentStatus) {
  return shipmentStatusLabels[status]
}

export function formatPaymentProvider(provider: PaymentProvider) {
  return paymentProviderLabels[provider]
}

export function getOrderStatusTone(status: OrderStatus) {
  if (status === 'DELIVERED' || status === 'PAID') return 'success'
  if (status === 'CANCELLED' || status === 'REFUNDED') return 'danger'
  if (status === 'SHIPPED' || status === 'PROCESSING') return 'progress'
  return 'pending'
}
