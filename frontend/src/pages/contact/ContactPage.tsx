import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { CheckIcon, MailIcon } from '../../components/common/Icons'
import { getApiError, isNetworkError } from '../../api/client'
import { contactApi } from '../../features/contact/api/contactApi'
import { contactSchema, type ContactFormValues } from '../../features/contact/schemas/contact.schema'
import { useAuth } from '../../features/auth/hooks/useAuth'

const topicOptions = [
  ['GENERAL', 'Întrebare generală'],
  ['ORDER', 'Comandă'],
  ['PAYMENT', 'Plată'],
  ['DELIVERY', 'Livrare'],
  ['ACCOUNT', 'Cont și autentificare'],
  ['LISTING', 'Anunț sau carte'],
  ['PRIVACY', 'Date personale'],
  ['OTHER', 'Alt subiect'],
] as const

export function ContactPage() {
  const { user } = useAuth()
  const [success, setSuccess] = useState<string | null>(null)
  const [requestError, setRequestError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ContactFormValues>({
    resolver: zodResolver(contactSchema),
    defaultValues: {
      name: user ? `${user.firstName} ${user.lastName}` : '',
      email: user?.email ?? '',
      topic: 'GENERAL',
      subject: '',
      message: '',
      privacyAccepted: false,
      website: '',
    },
  })

  useEffect(() => {
    if (!user) return
    reset({
      name: `${user.firstName} ${user.lastName}`,
      email: user.email,
      topic: 'GENERAL',
      subject: '',
      message: '',
      privacyAccepted: false,
      website: '',
    })
  }, [reset, user])

  const submit = handleSubmit(async (values) => {
    setSuccess(null)
    setRequestError(null)
    try {
      const response = await contactApi.send(values)
      setSuccess(response.message)
      reset({
        name: user ? `${user.firstName} ${user.lastName}` : '',
        email: user?.email ?? '',
        topic: 'GENERAL',
        subject: '',
        message: '',
        privacyAccepted: false,
        website: '',
      })
    } catch (error) {
      if (isNetworkError(error)) {
        setRequestError('Nu ne-am putut conecta la server. Verifică conexiunea și încearcă din nou.')
      } else if (getApiError(error)?.status === 429) {
        setRequestError('Ai trimis prea multe mesaje într-un timp scurt. Încearcă din nou mai târziu.')
      } else {
        setRequestError(getApiError(error)?.message ?? 'Mesajul nu a putut fi trimis momentan.')
      }
    }
  })

  return (
    <main className="contact-page">
      <section className="contact-hero">
        <div>
          <span className="contact-hero__tag">Suport BookNest</span>
          <h1>Cum te putem ajuta?</h1>
          <p>
            Spune-ne ce nu este clar sau ce nu funcționează. Mesajul ajunge direct la echipa
            BookNest și îți răspundem pe email.
          </p>
        </div>
      </section>

      <section className="contact-layout" aria-label="Contact BookNest">
        <aside className="contact-details">
          <div className="contact-details__icon"><MailIcon /></div>
          <h2>Scrie-ne direct</h2>
          <p>Poți folosi formularul sau adresa de mai jos, inclusiv pentru solicitări privind datele personale.</p>
          <a href="mailto:av.booknest@gmail.com">av.booknest@gmail.com</a>
          <div className="contact-details__note">
            <strong>Include detaliile utile</strong>
            <p>Pentru o comandă, menționează numărul ei. Nu trimite parole, date de card sau coduri de autentificare.</p>
          </div>
        </aside>

        <div className="contact-form-card">
          <header>
            <h2>Trimite un mesaj</h2>
            <p>Îți vom răspunde la adresa introdusă în formular.</p>
          </header>

          {success && <div className="contact-message contact-message--success" role="status"><CheckIcon />{success}</div>}
          {requestError && <div className="contact-message contact-message--error" role="alert">{requestError}</div>}

          <form className="contact-form" onSubmit={submit} noValidate>
            <div className="contact-form__row">
              <label>
                <span>Nume</span>
                <input type="text" autoComplete="name" {...register('name')} />
                {errors.name && <small>{errors.name.message}</small>}
              </label>
              <label>
                <span>Email pentru răspuns</span>
                <input type="email" autoComplete="email" {...register('email')} />
                {errors.email && <small>{errors.email.message}</small>}
              </label>
            </div>

            <label>
              <span>Cu ce te putem ajuta?</span>
              <select {...register('topic')}>
                {topicOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </label>

            <label>
              <span>Subiect</span>
              <input type="text" placeholder="Ex: Nu pot confirma o comandă" {...register('subject')} />
              {errors.subject && <small>{errors.subject.message}</small>}
            </label>

            <label>
              <span>Mesaj</span>
              <textarea rows={7} placeholder="Descrie situația și pașii pe care i-ai încercat..." {...register('message')} />
              {errors.message && <small>{errors.message.message}</small>}
            </label>

            <label className="contact-form__consent">
              <input type="checkbox" {...register('privacyAccepted')} />
              <span>Sunt de acord ca datele introduse să fie folosite pentru soluționarea mesajului, conform <Link to="/privacy">Politicii de confidențialitate</Link>.</span>
            </label>
            {errors.privacyAccepted && <small className="contact-form__standalone-error">{errors.privacyAccepted.message}</small>}

            <label className="contact-form__honeypot" aria-hidden="true">
              Website
              <input type="text" tabIndex={-1} autoComplete="off" {...register('website')} />
            </label>

            <button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Se trimite...' : 'Trimite mesajul'}
            </button>
          </form>
        </div>
      </section>
    </main>
  )
}
