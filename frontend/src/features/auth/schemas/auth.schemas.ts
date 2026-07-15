import { z } from 'zod'

const emailSchema = z
  .string()
  .trim()
  .min(1, 'Adresa de email este obligatorie.')
  .email('Introdu o adresă de email validă.')

const passwordSchema = z
  .string()
  .min(8, 'Parola trebuie să conțină cel puțin 8 caractere.')
  .max(72, 'Parola poate avea cel mult 72 de caractere.')
  .regex(/[A-Za-z]/, 'Parola trebuie să conțină o literă.')
  .regex(/\d/, 'Parola trebuie să conțină o cifră.')

export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Parola este obligatorie.'),
})

export const registerSchema = z
  .object({
    firstName: z
      .string()
      .trim()
      .min(1, 'Prenumele este obligatoriu.')
      .max(100, 'Prenumele este prea lung.'),
    lastName: z
      .string()
      .trim()
      .min(1, 'Numele este obligatoriu.')
      .max(100, 'Numele este prea lung.'),
    email: emailSchema,
    password: passwordSchema,
    confirmPassword: z.string().min(1, 'Confirmă parola.'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Parolele nu coincid.',
  })
