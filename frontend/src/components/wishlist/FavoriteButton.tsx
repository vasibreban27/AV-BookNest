import { useState } from 'react'
import { useAddToWishlist, useRemoveFromWishlist, useWishlist } from '../../features/wishlist/hooks/useWishlist'
import { getWishlistErrorMessage } from '../../features/wishlist/utils/wishlistErrors'
import { HeartIcon } from '../common/icons/AppIcons'
import type { FavoriteButtonProps } from './types/wishlist-component.types'

export function FavoriteButton({ bookId }: FavoriteButtonProps) {
  const { data: wishlist } = useWishlist()
  const addToWishlist = useAddToWishlist()
  const removeFromWishlist = useRemoveFromWishlist()
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const isFavorite = wishlist?.some((item) => item.book.id === bookId) ?? false
  const isPending = addToWishlist.isPending ||
    (removeFromWishlist.isPending && removeFromWishlist.variables === bookId)

  const handleToggle = async () => {
    setErrorMessage(null)
    try {
      if (isFavorite) {
        await removeFromWishlist.mutateAsync(bookId)
      } else {
        await addToWishlist.mutateAsync(bookId)
      }
    } catch (error) {
      setErrorMessage(getWishlistErrorMessage(error))
    }
  }

  return (
    <div className="favorite-action">
      <button
        className={`favorite-button${isFavorite ? ' favorite-button--active' : ''}`}
        type="button"
        onClick={handleToggle}
        disabled={isPending}
        aria-pressed={isFavorite}
        aria-label={isFavorite ? 'Elimină cartea din favorite' : 'Adaugă cartea la favorite'}
        title={isFavorite ? 'Elimină din favorite' : 'Adaugă la favorite'}
      >
        <HeartIcon />
      </button>
      {errorMessage && <span className="favorite-action__error" role="alert">{errorMessage}</span>}
    </div>
  )
}
