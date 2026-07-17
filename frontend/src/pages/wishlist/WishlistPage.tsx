import { BookCard } from '../../components/catalog/BookCard'
import { HeartIcon } from '../../components/common/icons/AppIcons'
import { WishlistEmptyState, WishlistErrorState, WishlistLoadingState } from '../../components/wishlist/WishlistStates'
import { useWishlist } from '../../features/wishlist/hooks/useWishlist'

export function WishlistPage() {
  const wishlistQuery = useWishlist()
  const itemCount = wishlistQuery.data?.length ?? 0

  return (
    <main className="wishlist-page">
      <section className="wishlist-hero">
        <div className="wishlist-page__container wishlist-hero__inner">
          <div>
            <span className="home-kicker">Raftul tău personal</span>
            <h1>Favorite</h1>
            <p>Cărțile care ți-au atras atenția, păstrate într-un singur loc.</p>
          </div>
          <span className="wishlist-hero__icon" aria-hidden="true"><HeartIcon /></span>
        </div>
      </section>

      <section className="wishlist-content">
        <div className="wishlist-page__container">
          {wishlistQuery.isLoading && <WishlistLoadingState />}
          {wishlistQuery.isError && (
            <WishlistErrorState onRetry={() => void wishlistQuery.refetch()} />
          )}
          {!wishlistQuery.isLoading && !wishlistQuery.isError && itemCount === 0 && (
            <WishlistEmptyState />
          )}
          {!wishlistQuery.isLoading && !wishlistQuery.isError && wishlistQuery.data && itemCount > 0 && (
            <>
              <div className="wishlist-heading">
                <div>
                  <span className="home-kicker">Salvate pentru mai târziu</span>
                  <h2>Colecția ta</h2>
                </div>
                <span>{itemCount} {itemCount === 1 ? 'carte favorită' : 'cărți favorite'}</span>
              </div>
              <div className="book-grid wishlist-grid">
                {wishlistQuery.data.map((item) => (
                  <BookCard book={item.book} key={item.book.id} />
                ))}
              </div>
            </>
          )}
        </div>
      </section>
    </main>
  )
}
