import { useState } from 'react'
import { useAuth } from '../../features/auth/hooks/useAuth'
import { useAddToCart, useCart } from '../../features/cart/hooks/useCart'
import { getCartErrorMessage } from '../../features/cart/utils/cartErrors'
import { CheckIcon } from '../common/Icons'
import { CartIcon } from '../common/icons/AppIcons'
import type { AddToCartButtonProps } from './types/cart-component.types'

export function AddToCartButton({ bookId, sellerId }: AddToCartButtonProps) {
  const { user } = useAuth()
  const { data: cart } = useCart()
  const addToCart = useAddToCart()
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const isOwnBook = user?.id === sellerId
  const isInCart = cart?.items.some((item) => item.book.id === bookId) ?? false

  const handleAdd = async () => {
    setErrorMessage(null)
    try {
      await addToCart.mutateAsync(bookId)
    } catch (error) {
      setErrorMessage(getCartErrorMessage(error))
    }
  }

  return (
    <div className="add-to-cart">
      <button
        className={`add-to-cart__button${isInCart ? ' add-to-cart__button--added' : ''}`}
        type="button"
        onClick={handleAdd}
        disabled={isOwnBook || isInCart || addToCart.isPending}
        title={isOwnBook ? 'Nu poți cumpăra propria carte' : undefined}
      >
        {isInCart ? <CheckIcon /> : <CartIcon />}
        <span>
          {isOwnBook
            ? 'Cartea ta'
            : isInCart
              ? 'În coș'
              : addToCart.isPending
                ? 'Se adaugă...'
                : 'Adaugă în coș'}
        </span>
      </button>
      {errorMessage && <span className="add-to-cart__error" role="alert">{errorMessage}</span>}
    </div>
  )
}
