import type { BookStatus } from '../../catalog/types/catalog.types'

const statusLabels: Record<BookStatus, string> = {
  DRAFT: 'Ciornă',
  AVAILABLE: 'Disponibilă',
  RESERVED: 'Rezervată',
  SOLD: 'Vândută',
  ARCHIVED: 'Arhivată',
}

export function formatListingStatus(status: BookStatus) {
  return statusLabels[status]
}

export function formatListingDate(value: string) {
  return new Intl.DateTimeFormat('ro-RO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(value))
}

export function getListingStatusTone(status: BookStatus) {
  return status.toLocaleLowerCase('en-US')
}
