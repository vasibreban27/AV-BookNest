export type OrderStatus =
  | 'PENDING'
  | 'PAID'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED'

export type PaymentProvider = 'STRIPE'
export type PaymentStatus =
  | 'PENDING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED'
  | 'PARTIALLY_REFUNDED'
  | 'REFUNDED'
export type SellerOrderStatus = 'AWAITING_SELLER' | 'ACCEPTED' | 'FULFILLED' | 'CANCELLED'
export type ShipmentStatus =
  | 'NOT_CREATED'
  | 'AWB_PENDING'
  | 'AWB_CREATED'
  | 'AWAITING_DROPOFF'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'RETURNED'
  | 'LOST'
  | 'CANCELLED'
export type PackageSize = 'S' | 'M' | 'L'

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
  easyboxId: string
  easyboxName: string
  easyboxAddress: string | null
  easyboxCity: string | null
  easyboxCounty: string | null
  easyboxPostalCode: string | null
  trackingNumber: string | null
  status: ShipmentStatus
  packageSize: PackageSize | null
  packageWeightGrams: number | null
  packageLengthMm: number | null
  packageWidthMm: number | null
  packageHeightMm: number | null
  providerStatus: string | null
  statusUpdatedAt: string | null
  labelUrl: string | null
  createdAt: string
}

export type SellerOrder = {
  id: number
  orderId: number
  orderNumber: string
  buyerName: string
  sellerId: number
  sellerName: string
  status: SellerOrderStatus
  itemSubtotal: number
  commissionRate: number
  commissionAmount: number
  sellerProceeds: number
  shippingCost: number
  acceptBy: string
  dropoffBy: string | null
  acceptedAt: string | null
  createdAt: string
  items: OrderItem[]
  shipment: Shipment
}

export type Order = {
  id: number
  orderNumber: string
  status: OrderStatus
  items: OrderItem[]
  sellerOrders: SellerOrder[]
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
  easyboxAddress: string
  easyboxCity: string
  easyboxCounty: string
  easyboxPostalCode: string
  recipientName: string
  recipientEmail: string
  recipientPhone: string
}

export type Easybox = {
  id: string
  name: string
  address: string
  city: string
  county: string
  postalCode: string
  latitude: number
  longitude: number
}

export type SellerShippingQuote = {
  sellerId: number
  cost: number
  packageSize: PackageSize
  weightGrams: number
  lengthMm: number
  widthMm: number
  heightMm: number
}

export type ShippingQuote = {
  shippingCost: number
  currency: string
  packages: SellerShippingQuote[]
}

export type OrderDetailsLocationState = {
  placed?: boolean
}
