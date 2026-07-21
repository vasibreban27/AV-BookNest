const MAX_COVER_SIZE = 5 * 1024 * 1024
const ACCEPTED_COVER_TYPES = ['image/jpeg', 'image/png', 'image/webp']

export function validateListingCover(file: File) {
  if (!ACCEPTED_COVER_TYPES.includes(file.type)) {
    return 'Alege o imagine JPEG, PNG sau WebP.'
  }
  if (file.size > MAX_COVER_SIZE) {
    return 'Imaginea nu poate depăși 5 MB.'
  }
  return null
}
