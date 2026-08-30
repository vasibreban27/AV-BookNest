import axios from 'axios'

export function formatRating(value: number | null) {
  return value == null ? '—' : new Intl.NumberFormat('ro-RO', { maximumFractionDigits: 1, minimumFractionDigits: 1 }).format(value)
}

export function reviewError(error: unknown) {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 409) return 'Recenzia există deja sau achiziția nu mai este eligibilă. Verifică informațiile actualizate.'
    if (error.response?.status === 403) return 'Nu ai permisiunea pentru această acțiune. Verifică sesiunea și drepturile contului.'
    if (error.response?.status === 404) return 'Achiziția sau recenzia nu a fost găsită.'
    if (error.response?.status === 400) return 'Verifică notele (1–5), lungimea comentariului și motivul moderării.'
  }
  return 'Nu am putut finaliza cererea. Încearcă din nou.'
}
