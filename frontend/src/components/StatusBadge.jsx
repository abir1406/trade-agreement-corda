export default function StatusBadge({ status }) {
  const map = {
    PENDING: 'bg-amber-100 text-amber-800 border-amber-200',
    APPROVED: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    REJECTED: 'bg-red-100 text-red-800 border-red-200',
  }
  const cls = map[status] || 'bg-slate-100 text-slate-700 border-slate-200'
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${cls}`}>
      <span className={`w-1.5 h-1.5 rounded-full mr-1.5 ${
        status === 'PENDING' ? 'bg-amber-500' :
        status === 'APPROVED' ? 'bg-emerald-500' :
        status === 'REJECTED' ? 'bg-red-500' : 'bg-slate-400'
      }`} />
      {status ?? 'UNKNOWN'}
    </span>
  )
}
