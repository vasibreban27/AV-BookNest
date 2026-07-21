import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { ListingCard } from '../../components/listings/ListingCard'
import { ListingsEmptyState, ListingsErrorState, ListingsLoadingState } from '../../components/listings/ListingStates'
import { BookOutlineIcon } from '../../components/common/icons/AppIcons'
import { useArchiveListing, useMyListings, usePublishListing } from '../../features/listings/hooks/useListings'
import { getListingErrorMessage } from '../../features/listings/utils/listingErrors'

type MyListingsLocationState = { created?: boolean; updated?: boolean }

export function MyListingsPage() {
  const location = useLocation()
  const locationState = location.state as MyListingsLocationState | null
  const listingsQuery = useMyListings()
  const archiveListing = useArchiveListing()
  const publishListing = usePublishListing()
  const [activeBookId, setActiveBookId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const handleArchive = async (bookId: number) => {
    if (activeBookId !== null) return
    setActionError(null)
    setActiveBookId(bookId)
    try {
      await archiveListing.mutateAsync(bookId)
    } catch (error) {
      setActionError(getListingErrorMessage(error))
    } finally {
      setActiveBookId(null)
    }
  }

  const handlePublish = async (bookId: number) => {
    if (activeBookId !== null) return
    setActionError(null)
    setActiveBookId(bookId)
    try {
      await publishListing.mutateAsync(bookId)
    } catch (error) {
      setActionError(getListingErrorMessage(error))
    } finally {
      setActiveBookId(null)
    }
  }

  const listings = listingsQuery.data ?? []
  const availableCount = listings.filter((book) => book.status === 'AVAILABLE').length

  return (
    <main className="listings-page">
      <section className="listings-hero">
        <div className="listings-page__container listings-hero__inner">
          <div><span className="home-kicker">Raftul tău de vânzare</span><h1>Cărțile mele</h1><p>Administrează anunțurile și urmărește starea fiecărei cărți.</p></div>
          <span className="listings-hero__icon" aria-hidden="true"><BookOutlineIcon /></span>
        </div>
      </section>

      <section className="listings-content">
        <div className="listings-page__container">
          {(locationState?.created || locationState?.updated) && (
            <div className="listings-success" role="status">{locationState.created ? 'Cartea a fost publicată cu succes.' : 'Modificările au fost salvate.'}</div>
          )}
          <div className="listings-heading">
            <div><span className="home-kicker">Inventar</span><h2>{listings.length} {listings.length === 1 ? 'carte' : 'cărți'} · {availableCount} disponibile</h2></div>
            <Link to="/sell">+ Vinde o carte</Link>
          </div>
          {actionError && <div className="listings-action-error" role="alert">{actionError}</div>}
          {listingsQuery.isLoading && <ListingsLoadingState />}
          {listingsQuery.isError && <ListingsErrorState onRetry={() => void listingsQuery.refetch()} />}
          {!listingsQuery.isLoading && !listingsQuery.isError && listings.length === 0 && <ListingsEmptyState />}
          {!listingsQuery.isLoading && !listingsQuery.isError && listings.length > 0 && (
            <div className="listings-grid">
              {listings.map((book) => (
                <ListingCard
                  book={book}
                  actionPending={activeBookId !== null}
                  onArchive={(id) => void handleArchive(id)}
                  onPublish={(id) => void handlePublish(id)}
                  key={book.id}
                />
              ))}
            </div>
          )}
        </div>
      </section>
    </main>
  )
}
