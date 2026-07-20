import type { OrderStatus, PaymentProvider, PaymentStatus, ShipmentStatus } from '../types/orders.types'

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
  PENDING: 'Plată la livrare',
  SUCCEEDED: 'Achitată',
  FAILED: 'Plată eșuată',
  CANCELLED: 'Plată anulată',
  REFUNDED: 'Rambursată',
}

const shipmentStatusLabels: Record<ShipmentStatus, string> = {
  AWAITING_SELLER: 'Așteaptă vânzătorul',
  AWB_CREATED: 'AWB creat',
  IN_TRANSIT: 'În tranzit',
  DELIVERED: 'Livrată',
  CANCELLED: 'Anulată',
}

const paymentProviderLabels: Record<PaymentProvider, string> = {
  CASH_ON_DELIVERY: 'Ramburs la Easybox',
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
