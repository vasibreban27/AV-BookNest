import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ListingForm } from '../../components/listings/ListingForm'
import { ListingFormLoadingState, ListingsErrorState } from '../../components/listings/ListingStates'
import type { ListingFormSubmission } from '../../components/listings/types/listing-component.types'
import { useListing, useListingCategories, useUpdateListing } from '../../features/listings/hooks/useListings'
import type { ListingEditLocationState } from '../../features/listings/types/listing.types'
import { getListingErrorMessage } from '../../features/listings/utils/listingErrors'

export function EditListingPage() {
  const { bookId: bookIdParam } = useParams()
  const bookId = Number(bookIdParam)
  const navigate = useNavigate()
  const location = useLocation()
  const locationState = location.state as ListingEditLocationState | null
  const listingQuery = useListing(bookId)
  const categoriesQuery = useListingCategories()
  const updateListing = useUpdateListing()
  const [submitError, setSubmitError] = useState<string | null>(
    locationState?.coverUploadFailed
      ? 'Cartea a fost publicată, dar coperta nu s-a încărcat. Selectează imaginea din nou și salvează.'
      : null,
  )

  if (!Number.isInteger(bookId) || bookId <= 0) return <Navigate to="/my-books" replace />

  const handleSubmit = async ({ payload, coverFile, removeCover }: ListingFormSubmission) => {
    setSubmitError(null)
    try {
      await updateListing.mutateAsync({ bookId, payload, coverFile, removeCover })
      navigate('/my-books', { replace: true, state: { updated: true } })
    } catch (error) {
      setSubmitError(getListingErrorMessage(error))
    }
  }

  const isLoading = listingQuery.isLoading || categoriesQuery.isLoading
  const isError = listingQuery.isError || categoriesQuery.isError
  const book = listingQuery.data

  return (
    <main className="listing-editor-page">
      <section className="listing-editor-content">
        <div className="listings-page__container">
          <Link className="listing-editor__back" to="/my-books">← Înapoi la cărțile mele</Link>
          {isLoading && <ListingFormLoadingState />}
          {isError && <ListingsErrorState onRetry={() => { void listingQuery.refetch(); void categoriesQuery.refetch() }} />}
          {book && (book.status === 'RESERVED' || book.status === 'SOLD') && (
            <div className="listings-state"><h2>Această carte nu mai poate fi modificată.</h2><p>{book.status === 'RESERVED' ? 'Cartea este rezervată într-o comandă activă.' : 'Cartea a fost deja vândută.'}</p><Link to="/my-books">Vezi cărțile mele</Link></div>
          )}
          {book && categoriesQuery.data && book.status !== 'RESERVED' && book.status !== 'SOLD' && (
            <ListingForm
              categories={categoriesQuery.data}
              initialBook={book}
              mode="edit"
              submitError={submitError}
              isPending={updateListing.isPending}
              onSubmit={handleSubmit}
            />
          )}
        </div>
      </section>
    </main>
  )
}
