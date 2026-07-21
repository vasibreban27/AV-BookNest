import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useRef, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { BookOutlineIcon, UploadIcon } from '../common/icons/AppIcons'
import { formatBookCondition, formatBookPrice } from '../../features/catalog/utils/catalogFormatters'
import { listingSchema } from '../../features/listings/schemas/listing.schema'
import type { ListingFormValues } from '../../features/listings/types/listing-form.types'
import { validateListingCover } from '../../features/listings/utils/listingCoverValidation'
import type { ListingFormProps } from './types/listing-component.types'

const conditionOptions = ['NEW', 'LIKE_NEW', 'VERY_GOOD', 'GOOD', 'ACCEPTABLE'] as const

export function ListingForm({
  categories,
  initialBook,
  mode,
  submitError,
  isPending,
  onSubmit,
}: ListingFormProps) {
  const [coverFile, setCoverFile] = useState<File | null>(null)
  const [coverPreview, setCoverPreview] = useState<string | null>(null)
  const [coverError, setCoverError] = useState<string | null>(null)
  const [removeCover, setRemoveCover] = useState(false)
  const coverInputRef = useRef<HTMLInputElement>(null)
  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ListingFormValues>({
    resolver: zodResolver(listingSchema),
    defaultValues: {
      title: initialBook?.title ?? '',
      author: initialBook?.author ?? '',
      isbn: initialBook?.isbn ?? '',
      description: initialBook?.description ?? '',
      price: initialBook?.price ?? 0,
      bookCondition: initialBook?.bookCondition ?? 'GOOD',
      language: initialBook?.language ?? 'Română',
      publisher: initialBook?.publisher ?? '',
      publishedYear: initialBook?.publishedYear ?? undefined,
      categoryId: initialBook?.category.id ?? categories[0]?.id,
    },
  })
  const previewValues = useWatch({ control })

  useEffect(() => {
    return () => {
      if (coverPreview) URL.revokeObjectURL(coverPreview)
    }
  }, [coverPreview])

  const displayedCover = coverPreview ?? (!removeCover ? initialBook?.coverImageUrl : null)
  const selectedCategory = categories.find((category) => category.id === previewValues.categoryId)

  const handleCoverChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return
    const validationError = validateListingCover(file)
    if (validationError) {
      setCoverError(validationError)
      event.target.value = ''
      return
    }
    setCoverError(null)
    setRemoveCover(false)
    setCoverFile(file)
    setCoverPreview(URL.createObjectURL(file))
  }

  const clearCover = () => {
    setCoverFile(null)
    setCoverPreview(null)
    setCoverError(null)
    if (coverInputRef.current) coverInputRef.current.value = ''
    if (initialBook?.coverImageUrl) setRemoveCover(true)
  }

  const submit = handleSubmit(async (values) => {
    await onSubmit({
      payload: {
        title: values.title.trim(),
        author: values.author.trim(),
        isbn: values.isbn.trim() || null,
        description: values.description.trim() || null,
        price: values.price,
        bookCondition: values.bookCondition,
        language: values.language.trim(),
        publisher: values.publisher.trim() || null,
        publishedYear: values.publishedYear ?? null,
        categoryId: values.categoryId,
      },
      coverFile,
      removeCover,
    })
  })

  return (
    <form className="listing-form" onSubmit={submit} noValidate>
      <div className="listing-form__fields">
        <div className="listing-form__intro">
          <p className="home-kicker">{mode === 'create' ? 'Anunț nou' : 'Editează anunțul'}</p>
          <h1>{mode === 'create' ? 'Dă cărții tale un nou cititor.' : 'Actualizează detaliile cărții.'}</h1>
          <p>Completează informațiile cât mai clar pentru ca viitorul cumpărător să știe exact ce primește.</p>
        </div>

        {submitError && <div className="listing-form__error" role="alert">{submitError}</div>}

        <fieldset className="listing-form__section">
          <legend>Despre carte</legend>
          <div className="listing-form__grid">
            <label className="listing-field listing-field--wide">
              <span>Titlu *</span>
              <input autoFocus type="text" placeholder="ex. Micul Prinț" aria-invalid={Boolean(errors.title)} {...register('title')} />
              {errors.title && <small>{errors.title.message}</small>}
            </label>
            <label className="listing-field listing-field--wide">
              <span>Autor *</span>
              <input type="text" placeholder="ex. Antoine de Saint-Exupéry" aria-invalid={Boolean(errors.author)} {...register('author')} />
              {errors.author && <small>{errors.author.message}</small>}
            </label>
            <label className="listing-field">
              <span>Categorie *</span>
              <select aria-invalid={Boolean(errors.categoryId)} {...register('categoryId', { valueAsNumber: true })}>
                {categories.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}
              </select>
              {errors.categoryId && <small>{errors.categoryId.message}</small>}
            </label>
            <label className="listing-field">
              <span>Limba *</span>
              <input type="text" aria-invalid={Boolean(errors.language)} {...register('language')} />
              {errors.language && <small>{errors.language.message}</small>}
            </label>
            <label className="listing-field">
              <span>Editura</span>
              <input type="text" placeholder="Opțional" aria-invalid={Boolean(errors.publisher)} {...register('publisher')} />
              {errors.publisher && <small>{errors.publisher.message}</small>}
            </label>
            <label className="listing-field">
              <span>Anul publicării</span>
              <input
                type="number"
                min="1000"
                max={new Date().getFullYear()}
                placeholder="Opțional"
                aria-invalid={Boolean(errors.publishedYear)}
                {...register('publishedYear', { setValueAs: (value: string) => value === '' ? undefined : Number(value) })}
              />
              {errors.publishedYear && <small>{errors.publishedYear.message}</small>}
            </label>
            <label className="listing-field listing-field--wide">
              <span>ISBN</span>
              <input type="text" placeholder="Opțional" aria-invalid={Boolean(errors.isbn)} {...register('isbn')} />
              {errors.isbn && <small>{errors.isbn.message}</small>}
            </label>
            <label className="listing-field listing-field--wide">
              <span>Descriere</span>
              <textarea rows={6} placeholder="Starea coperții, eventuale adnotări sau alte detalii utile..." aria-invalid={Boolean(errors.description)} {...register('description')} />
              {errors.description && <small>{errors.description.message}</small>}
            </label>
          </div>
        </fieldset>

        <fieldset className="listing-form__section">
          <legend>Stare și preț</legend>
          <div className="listing-form__grid">
            <label className="listing-field">
              <span>Starea cărții *</span>
              <select {...register('bookCondition')}>
                {conditionOptions.map((condition) => <option value={condition} key={condition}>{formatBookCondition(condition)}</option>)}
              </select>
            </label>
            <label className="listing-field">
              <span>Preț (RON) *</span>
              <input type="number" min="0" step="0.01" aria-invalid={Boolean(errors.price)} {...register('price', { valueAsNumber: true })} />
              {errors.price && <small>{errors.price.message}</small>}
            </label>
          </div>
        </fieldset>

        <fieldset className="listing-form__section">
          <legend>Coperta</legend>
          <label className="listing-cover-upload">
            <UploadIcon />
            <strong>{coverFile ? coverFile.name : 'Alege o fotografie'}</strong>
            <span>JPEG, PNG sau WebP · maximum 5 MB</span>
            <input ref={coverInputRef} type="file" accept="image/jpeg,image/png,image/webp" onChange={handleCoverChange} />
          </label>
          {coverError && <small className="listing-cover-error">{coverError}</small>}
          {displayedCover && <button className="listing-cover-remove" type="button" onClick={clearCover}>Elimină coperta selectată</button>}
          {removeCover && initialBook?.coverImageUrl && (
            <button className="listing-cover-restore" type="button" onClick={() => setRemoveCover(false)}>Păstrează coperta existentă</button>
          )}
        </fieldset>

        <button className="listing-submit" type="submit" disabled={isSubmitting || isPending}>
          {isSubmitting || isPending ? 'Salvăm anunțul...' : mode === 'create' ? 'Publică anunțul' : 'Salvează modificările'}
        </button>
      </div>

      <aside className="listing-preview" aria-label="Previzualizarea anunțului">
        <p className="home-kicker">Previzualizare</p>
        <div className="listing-preview__cover">
          {displayedCover ? <img src={displayedCover} alt="Previzualizarea coperții" /> : <span><BookOutlineIcon /><small>Adaugă o copertă</small></span>}
        </div>
        <span>{selectedCategory?.name ?? 'Categorie'}</span>
        <h2>{previewValues.title || 'Titlul cărții'}</h2>
        <p>de {previewValues.author || 'Autor'}</p>
        <div>
          <small>{formatBookCondition(previewValues.bookCondition ?? 'GOOD')}</small>
          <strong>{formatBookPrice(Number.isFinite(previewValues.price) ? previewValues.price ?? 0 : 0)}</strong>
        </div>
      </aside>
    </form>
  )
}
