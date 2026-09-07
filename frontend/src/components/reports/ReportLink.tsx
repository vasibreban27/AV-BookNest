import { Link } from 'react-router-dom'
import { useAuth } from '../../features/auth/hooks/useAuth'
import type { ReportTarget } from '../../features/reports/api'
import { FlagIcon } from '../common/icons/AppIcons'
import '../../styles/components/reports/report-link.css'

export function ReportLink({ targetType, targetId, ownerId }: { targetType: ReportTarget; targetId: number; ownerId: number }) {
  const { user } = useAuth()
  if (user?.id === ownerId) return null
  return <Link to={`/report?targetType=${targetType}&targetId=${targetId}`} className="report-link"><FlagIcon /><span>{targetType === 'BOOK' ? 'Raportează anunțul' : 'Raportează utilizatorul'}</span></Link>
}
