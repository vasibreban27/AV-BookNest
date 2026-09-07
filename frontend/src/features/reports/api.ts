import { api, getApiError } from '../../api/client'
import type { PageResponse } from '../admin/types/admin.types'

export const reportReasons = { SPAM: 'Spam sau publicitate', FRAUD: 'Suspiciune de fraudă', PROHIBITED_CONTENT: 'Conținut interzis', HARASSMENT: 'Hărțuire sau limbaj abuziv', MISLEADING: 'Informații înșelătoare', OTHER: 'Alt motiv' } as const
export const reportStatuses = { NEW: 'Nouă', IN_PROGRESS: 'În lucru', RESOLVED: 'Soluționată', DISMISSED: 'Respinsă' } as const
export const reportDecisions = { DISMISS: 'Respinge raportarea', WARN: 'Avertizează utilizatorul', HIDE_BOOK: 'Ascunde anunțul', SUSPEND: 'Suspendă temporar contul' } as const
export type ReportReason = keyof typeof reportReasons
export type ReportStatus = keyof typeof reportStatuses
export type ReportDecision = keyof typeof reportDecisions
export type ReportTarget = 'BOOK' | 'USER'
export type ContentReport = { id: number; reporterId: number; targetType: ReportTarget; targetUserId: number; bookId: number | null; targetLabel: string; reason: ReportReason; description: string; status: ReportStatus; assignedToId: number | null; decision: ReportDecision | null; decisionNote: string | null; suspendedUntil: string | null; createdAt: string; updatedAt: string }
export type ReportDetails = { report: ContentReport; evidence: { id: number; contentType: string }[]; events: { id: number; actorId: number; action: string; note: string; createdAt: string }[] }
export type AccountHistory = { enabled: boolean; suspendedUntil: string | null; suspensionReason: string | null; entries: { action: string; reason: string | null; createdAt: string }[] }
export type ReportResolution = { decision: ReportDecision; note: string; suspensionDays?: number }
const base = (admin: boolean) => admin ? '/admin/reports' : '/reports'
export const reportsApi = {
  async create(targetType: ReportTarget, targetId: number, reason: ReportReason, description: string, files: File[]) {
    const form = new FormData()
    form.append('report', new Blob([JSON.stringify({ targetType, targetId, reason, description })], { type: 'application/json' }))
    files.forEach(file => form.append('evidence', file))
    return (await api.post<ContentReport>('/reports', form, { headers: { 'Content-Type': undefined } })).data
  },
  async list(admin: boolean, page: number, status: ReportStatus | '', targetUserId?: number) {
    return (await api.get<PageResponse<ContentReport>>(base(admin), { params: { page, size: 20, status: status || undefined, targetUserId } })).data
  },
  async details(admin: boolean, id: number) { return (await api.get<ReportDetails>(`${base(admin)}/${id}`)).data },
  async claim(id: number) { return (await api.post<ReportDetails>(`/admin/reports/${id}/claim`)).data },
  async resolve(id: number, body: ReportResolution) { return (await api.post<ReportDetails>(`/admin/reports/${id}/resolve`, body)).data },
  async history(userId: number) { return (await api.get<AccountHistory>(`/admin/reports/users/${userId}/history`)).data },
  async evidence(reportId: number, id: number, signal: AbortSignal) {
    return (await api.get<Blob>(`/reports/${reportId}/evidence/${id}`, { responseType: 'blob', signal })).data
  },
}
export function evidenceError(files: File[]) {
  if (files.length > 3) return 'Poți atașa maximum 3 fotografii.'
  if (files.some(file => !['image/jpeg', 'image/png'].includes(file.type))) return 'Alege doar fotografii PNG sau JPEG.'
  if (files.some(file => file.size === 0 || file.size > 2 * 1024 * 1024)) return 'Fiecare fotografie trebuie să aibă cel mult 2 MB și să nu fie goală.'
  return null
}
export function reportError(error: unknown) {
  switch (getApiError(error)?.status) {
    case 400: return 'Verifică datele: fotografii PNG/JPEG de maximum 2 MB, cel mult 4096 px / 12 megapixeli și o durată validă a suspendării.'
    case 403: return 'Nu ai permisiunea necesară pentru această acțiune.'
    case 404: return 'Raportarea sau conținutul nu mai este disponibil ori nu ai acces.'
    case 409: return 'Există deja o raportare deschisă, raportarea a fost soluționată sau contul este deja suspendat. Verifică starea actuală înainte de a reîncerca.'
    case 413: return 'Fișierele sunt prea mari. Alege cel mult 3 fotografii de maximum 2 MB fiecare.'
    case 429: return 'Ai atins limita de 10 raportări în 24 de ore. Încearcă din nou mai târziu.'
    default: return 'Nu am putut finaliza acțiunea. Verifică conexiunea și încearcă din nou.'
  }
}
