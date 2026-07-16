import { z } from 'zod'
import type {
  loginSchema,
  registerSchema,
} from '../schemas/auth.schemas'

export type LoginFormValues = z.infer<typeof loginSchema>
export type RegisterFormValues = z.infer<typeof registerSchema>
