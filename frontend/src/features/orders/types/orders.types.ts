export type OrderStatus =
  | 'PENDING'
  | 'PAID'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED'

export type PaymentProvider = 'CASH_ON_DELIVERY'
export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'REFUNDED'
export type ShipmentStatus = 'AWAITING_SELLER' | 'AWB_CREATED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELLED'

export type OrderItem = {
  id: number
  bookId: number | null
  sellerId: number
  title: string
  author: string
  isbn: string | null
  unitPrice: number
  quantity: number
}

export type Payment = {
  id: number
  provider: PaymentProvider
  amount: number
  currency: string
  status: PaymentStatus
}

export type Shipment = {
  id: number
  sellerId: number
  sellerName: string
  easyboxId: string
  easyboxName: string
  trackingNumber: string | null
  status: ShipmentStatus
  codAmount: number
  items: OrderItem[]
}

export type Order = {
  id: number
  orderNumber: string
  status: OrderStatus
  items: OrderItem[]
  shipments: Shipment[]
  subtotal: number
  shippingCost: number
  totalAmount: number
  currency: string
  placedAt: string
  payment: Payment | null
}

export type CheckoutPayload = {
  easyboxId: string
  easyboxName: string
}

export type OrderDetailsLocationState = {
  placed?: boolean
}
