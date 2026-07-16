import type { BookCondition } from '../types/catalog.types'

const conditionLabels: Record<BookCondition, string> = {
  NEW: 'Nouă',
  LIKE_NEW: 'Ca nouă',
  VERY_GOOD: 'Foarte bună',
  GOOD: 'Bună',
  ACCEPTABLE: 'Acceptabilă',
}

export function normalizeCatalogText(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('ro-RO')
    .trim()
}

export function formatBookPrice(price: number) {
  return new Intl.NumberFormat('ro-RO', {
    style: 'currency',
    currency: 'RON',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(price)
}

export function formatBookCondition(condition: BookCondition) {
  return conditionLabels[condition]
}

export function getBookCoverTone(bookId: number) {
  return ['sage', 'clay', 'gold', 'berry', 'ocean'][bookId % 5]
}
