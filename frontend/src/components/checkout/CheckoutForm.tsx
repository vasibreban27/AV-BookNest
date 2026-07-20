import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useCheckout } from '../../features/orders/hooks/useOrders'
import { checkoutSchema } from '../../features/orders/schemas/checkout.schema'
import type { CheckoutFormValues } from '../../features/orders/types/orders-form.types'
import { getOrderErrorMessage } from '../../features/orders/utils/orderErrors'
import { PackageIcon, PinIcon } from '../common/icons/AppIcons'
import type { CheckoutFormProps } from './types/checkout-component.types'

export function CheckoutForm({ onCompleted }: CheckoutFormProps) {
  const checkout = useCheckout()
  const [submitError, setSubmitError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CheckoutFormValues>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: { easyboxId: '', easyboxName: '' },
  })

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
          <p>Introdu codul și denumirea lockerului în care dorești să primești cărțile.</p>
        </div>
      </div>

      {submitError && <div className="checkout-form__error" role="alert">{submitError}</div>}

      <div className="checkout-field">
        <label htmlFor="easyboxId">Cod Easybox</label>
        <input
          id="easyboxId"
          type="text"
          placeholder="ex. RO1234"
          aria-invalid={Boolean(errors.easyboxId)}
          aria-describedby={errors.easyboxId ? 'easyboxId-error' : undefined}
          autoFocus
          {...register('easyboxId')}
        />
        {errors.easyboxId && <span id="easyboxId-error">{errors.easyboxId.message}</span>}
      </div>

      <div className="checkout-field">
        <label htmlFor="easyboxName">Nume sau adresă Easybox</label>
        <input
          id="easyboxName"
          type="text"
          placeholder="ex. Easybox Calea Victoriei 100, București"
          aria-invalid={Boolean(errors.easyboxName)}
          aria-describedby={errors.easyboxName ? 'easyboxName-error' : undefined}
          {...register('easyboxName')}
        />
        {errors.easyboxName && <span id="easyboxName-error">{errors.easyboxName.message}</span>}
      </div>

      <div className="checkout-payment-note">
        <span><PackageIcon /></span>
        <div>
          <strong>Plată ramburs</strong>
          <p>Vei achita valoarea fiecărui colet la ridicarea din Easybox.</p>
        </div>
      </div>

      <button className="checkout-submit" type="submit" disabled={isSubmitting || checkout.isPending}>
        {isSubmitting || checkout.isPending ? 'Plasăm comanda...' : 'Plasează comanda'}
      </button>
      <small className="checkout-form__legal">Prin plasarea comenzii confirmi că datele Easybox sunt corecte.</small>
    </form>
  )
}
