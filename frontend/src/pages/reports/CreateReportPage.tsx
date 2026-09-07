import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { evidenceError, reportError, reportReasons, reportsApi, type ReportReason } from '../../features/reports/api'
import { referenceId } from '../../features/support/utils'
import '../../styles/pages/reports/reports.css'

export function CreateReportPage() {
  const [params] = useSearchParams()
  const targetType = params.get('targetType')
  const targetId = referenceId(params.get('targetId'))
  const [reason, setReason] = useState<ReportReason>('SPAM')
  const [description, setDescription] = useState('')
  const [files, setFiles] = useState<File[]>([])
  const [validation, setValidation] = useState<string | null>(null)
  const navigate = useNavigate()
  const client = useQueryClient()
  const mutation = useMutation({ mutationFn: () => reportsApi.create(targetType as 'BOOK' | 'USER', targetId!, reason, description.trim(), files), onSuccess: async report => {
    await client.invalidateQueries({ queryKey: ['reports'] })
    navigate(`/reports/${report.id}`, { replace: true })
  } })
  if (!targetId || (targetType !== 'BOOK' && targetType !== 'USER')) return <main className="reports-page"><h1>Alege conținutul de raportat</h1><p>Folosește butonul de raportare de lângă anunț sau utilizator.</p><Link to="/catalog">Înapoi la catalog</Link></main>
  return <main className="reports-page">
    <Link to="/reports">← Raportările mele</Link>
    <header><span className="home-kicker">Siguranța comunității</span><h1>{targetType === 'BOOK' ? 'Raportează anunțul' : 'Raportează utilizatorul'}</h1><p>{targetType === 'BOOK' ? 'Anunț' : 'Utilizator'} #{targetId}. Echipa verifică fiecare raportare înainte de a lua măsuri.</p></header>
    <form className="report-card report-form" onSubmit={event => {
      event.preventDefault()
      const error = evidenceError(files) || (reason === 'OTHER' && description.trim().length < 10 ? 'Descrie problema în cel puțin 10 caractere pentru „Alt motiv”.' : null)
      setValidation(error)
      if (!error) mutation.mutate()
    }}>
      <label>Motivul raportării<select value={reason} disabled={mutation.isPending} onChange={e => setReason(e.target.value as ReportReason)}>{Object.entries(reportReasons).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <label>Descriere suplimentară<textarea rows={5} maxLength={2000} required={reason === 'OTHER'} value={description} disabled={mutation.isPending} onChange={e => setDescription(e.target.value)} aria-describedby="report-description-help" /></label>
      <small id="report-description-help">{description.length}/2000 caractere. Precizează ce s-a întâmplat; nu include parole sau date de plată.</small>
      <label>Fotografii / dovezi (opțional)<input type="file" accept="image/png,image/jpeg" multiple disabled={mutation.isPending} aria-describedby="report-evidence-help" onChange={e => { const selected = Array.from(e.target.files ?? []); setFiles(selected); setValidation(evidenceError(selected)) }} /></label>
      <small id="report-evidence-help">Maximum 3 fotografii PNG/JPEG, câte 2 MB, cel mult 4096 px și 12 megapixeli. Sunt private, vizibile doar ție și administratorilor. Metadatele fotografiilor sunt eliminate.</small>
      {files.length > 0 && <ul className="report-files">{files.map((file, i) => <li key={`${file.name}-${i}`}><span>{file.name}</span><button type="button" disabled={mutation.isPending} onClick={() => { const next = files.filter((_, index) => index !== i); setFiles(next); setValidation(evidenceError(next)) }} aria-label={`Elimină ${file.name}`}>Elimină</button></li>)}</ul>}
      <p>Identitatea ta nu este afișată utilizatorului raportat. Raportările neîntemeiate nu reprezintă abateri confirmate.</p>
      {validation && <p role="alert" className="report-error">{validation}</p>}
      {mutation.isError && <p role="alert" className="report-error">{reportError(mutation.error)}</p>}
      <button className="report-primary" type="submit" disabled={mutation.isPending}>{mutation.isPending ? 'Se trimite…' : 'Trimite raportarea'}</button>
    </form>
  </main>
}
