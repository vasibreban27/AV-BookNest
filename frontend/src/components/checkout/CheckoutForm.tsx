import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useAuth } from '../../features/auth/hooks/useAuth'
import { useCheckout } from '../../features/orders/hooks/useOrders'
import { checkoutSchema } from '../../features/orders/schemas/checkout.schema'
import type { CheckoutFormValues } from '../../features/orders/types/orders-form.types'
import type { Easybox } from '../../features/orders/types/orders.types'
import { getOrderErrorMessage } from '../../features/orders/utils/orderErrors'
import { useEasyboxes } from '../../features/shipping/hooks/useEasyboxes'
import { PackageIcon, PinIcon } from '../common/icons/AppIcons'
import type { CheckoutFormProps } from './types/checkout-component.types'

export function CheckoutForm({ onCompleted }: CheckoutFormProps) {
  const { user } = useAuth()
  const checkout = useCheckout()
  const [search, setSearch] = useState('')
  const [selectedEasybox, setSelectedEasybox] = useState<Easybox | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const easyboxesQuery = useEasyboxes(search)
  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<CheckoutFormValues>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: {
      easyboxId: '',
      easyboxName: '',
      easyboxAddress: '',
      easyboxCity: '',
      easyboxCounty: '',
      easyboxPostalCode: '',
      recipientName: user ? `${user.firstName} ${user.lastName}` : '',
      recipientEmail: user?.email ?? '',
      recipientPhone: '',
    },
  })

  const selectEasybox = (easybox: Easybox) => {
    setSelectedEasybox(easybox)
    setValue('easyboxId', easybox.id, { shouldValidate: true })
    setValue('easyboxName', easybox.name)
    setValue('easyboxAddress', easybox.address)
    setValue('easyboxCity', easybox.city)
    setValue('easyboxCounty', easybox.county)
    setValue('easyboxPostalCode', easybox.postalCode)
  }

  const onSubmit = handleSubmit(async (values) => {
    setSubmitError(null)
    try {
      const order = await checkout.mutateAsync(values)
      onCompleted(order)
    } catch (error) {
      setSubmitError(getOrderErrorMessage(error))
    }
  })

  return (
    <form className="checkout-form" onSubmit={onSubmit} noValidate>
      <div className="checkout-form__heading">
        <span><PinIcon /></span>
        <div>
          <p className="home-kicker">Pasul 1</p>
          <h2>Alege destinația Easybox</h2>
          <p>Caută după oraș, adresă sau denumire. Lista este furnizată de Sameday.</p>
        </div>
      </div>

      {submitError && <div className="checkout-form__error" role="alert">{submitError}</div>}

      <div className="checkout-field">
        <label htmlFor="easybox-search">Caută Easybox</label>
        <input
          id="easybox-search"
          type="search"
          placeholder="ex. Cluj Mărăști"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value)
            setSelectedEasybox(null)
            setValue('easyboxId', '')
          }}
          autoFocus
        />
        {errors.easyboxId && <span>{errors.easyboxId.message}</span>}
      </div>

      {easyboxesQuery.isFetching && <p className="checkout-easybox-note">Căutăm în rețeaua Sameday...</p>}
      {easyboxesQuery.isError && (
        <p className="checkout-form__error">Lista Easybox nu este disponibilă. Verifică configurarea Sameday.</p>
      )}
      {easyboxesQuery.data && easyboxesQuery.data.length > 0 && !selectedEasybox && (
        <div className="checkout-easybox-results">
          {easyboxesQuery.data.map((easybox) => (
            <button type="button" key={easybox.id} onClick={() => selectEasybox(easybox)}>
              <strong>{easybox.name}</strong>
              <span>{easybox.address}, {easybox.city}, {easybox.county}</span>
            </button>
          ))}
        </div>
      )}
      {selectedEasybox && (
        <div className="checkout-easybox-selected">
          <PinIcon />
          <span><strong>{selectedEasybox.name}</strong><small>{selectedEasybox.address}, {selectedEasybox.city}</small></span>
          <button type="button" onClick={() => setSelectedEasybox(null)}>Schimbă</button>
        </div>
      )}

      <input type="hidden" {...register('easyboxId')} />
      <input type="hidden" {...register('easyboxName')} />
      <input type="hidden" {...register('easyboxAddress')} />
      <input type="hidden" {...register('easyboxCity')} />
      <input type="hidden" {...register('easyboxCounty')} />
      <input type="hidden" {...register('easyboxPostalCode')} />

      <div className="checkout-field">
        <label htmlFor="recipientName">Destinatar</label>
        <input id="recipientName" {...register('recipientName')} />
        {errors.recipientName && <span>{errors.recipientName.message}</span>}
      </div>
      <div className="checkout-field">
        <label htmlFor="recipientEmail">Email pentru notificările Sameday</label>
        <input id="recipientEmail" type="email" {...register('recipientEmail')} />
        {errors.recipientEmail && <span>{errors.recipientEmail.message}</span>}
      </div>
      <div className="checkout-field">
        <label htmlFor="recipientPhone">Telefon</label>
        <input id="recipientPhone" type="tel" placeholder="+40 7..." {...register('recipientPhone')} />
        {errors.recipientPhone && <span>{errors.recipientPhone.message}</span>}
      </div>

      <div className="checkout-payment-note">
        <span><PackageIcon /></span>
        <div>
          <strong>Plată online securizată</strong>
          <p>Plata va fi procesată prin Stripe; AWB-ul Sameday are ramburs 0.</p>
        </div>
      </div>

      <button className="checkout-submit" type="submit" disabled={isSubmitting || checkout.isPending}>
        {isSubmitting || checkout.isPending ? 'Pregătim plata...' : 'Continuă către plată'}
      </button>
      <small className="checkout-form__legal">Transportul se calculează din oferta Sameday Basic pentru fiecare vânzător.</small>
    </form>
  )
}
