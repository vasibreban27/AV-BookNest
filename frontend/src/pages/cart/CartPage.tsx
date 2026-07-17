import { useState } from 'react'
import { CartItemCard } from '../../components/cart/CartItemCard'
import { CartEmptyState, CartErrorState, CartLoadingState } from '../../components/cart/CartStates'
import { CartSummary } from '../../components/cart/CartSummary'
import { CartIcon } from '../../components/common/icons/AppIcons'
import { useCart, useClearCart, useRemoveFromCart } from '../../features/cart/hooks/useCart'
import { getCartErrorMessage } from '../../features/cart/utils/cartErrors'

export function CartPage() {
  const cartQuery = useCart()
  const removeFromCart = useRemoveFromCart()
  const clearCart = useClearCart()
  const [actionError, setActionError] = useState<string | null>(null)

  const handleRemove = async (bookId: number) => {
    setActionError(null)
    try {
      await removeFromCart.mutateAsync(bookId)
    } catch (error) {
      setActionError(getCartErrorMessage(error))
    }
  }

  const handleClear = async () => {
    setActionError(null)
    try {
      await clearCart.mutateAsync()
    } catch (error) {
      setActionError(getCartErrorMessage(error))
    }
  }

  return (
    <main className="cart-page">
      <section className="cart-hero">
        <div className="cart-page__container cart-hero__inner">
          <div>
            <span className="home-kicker">Selecția ta BookNest</span>
            <h1>Coșul meu</h1>
            <p>Cărțile alese de tine, pregătite pentru următorul capitol.</p>
          </div>
          <span className="cart-hero__icon" aria-hidden="true"><CartIcon /></span>
        </div>
      </section>

      <section className="cart-content">
        <div className="cart-page__container">
          {actionError && <div className="cart-action-error" role="alert">{actionError}</div>}

          {cartQuery.isLoading && <CartLoadingState />}
          {cartQuery.isError && <CartErrorState onRetry={() => void cartQuery.refetch()} />}
          {!cartQuery.isLoading && !cartQuery.isError && cartQuery.data?.items.length === 0 && (
            <CartEmptyState />
          )}

          {!cartQuery.isLoading && !cartQuery.isError && cartQuery.data && cartQuery.data.items.length > 0 && (
            <div className="cart-layout">
              <div className="cart-items">
                <div className="cart-items__heading">
                  <h2>Cărți în coș</h2>
                  <span>{cartQuery.data.items.length} {cartQuery.data.items.length === 1 ? 'volum' : 'volume'}</span>
                </div>
                {cartQuery.data.items.map((item) => (
                  <CartItemCard
                    item={item}
                    key={item.id}
                    isRemoving={removeFromCart.isPending && removeFromCart.variables === item.book.id}
                    onRemove={handleRemove}
                  />
                ))}
              </div>

              <CartSummary
                itemCount={cartQuery.data.items.length}
                total={cartQuery.data.total}
                isClearing={clearCart.isPending}
                onClear={handleClear}
              />
            </div>
          )}
        </div>
      </section>
    </main>
  )
}
