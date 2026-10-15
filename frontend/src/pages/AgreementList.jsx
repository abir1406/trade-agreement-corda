import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAgreements } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import LoadingSpinner from '../components/LoadingSpinner'
import {
  Search,
  ArrowUpDown,
  ArrowRight,
  FileText,
  Hash,
  Filter,
} from 'lucide-react'

const STATUSES = ['ALL', 'PENDING', 'APPROVED', 'REJECTED']

export default function AgreementList() {
  const [agreements, setAgreements] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [sortField, setSortField] = useState('createdAt')
  const [sortDir, setSortDir] = useState('desc')

  useEffect(() => {
    getAgreements()
      .then(setAgreements)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  const shortOrg = (name) => {
    if (!name) return '?'
    const m = name.match(/O=([^,]+)/)
    return m ? m[1] : name
  }

  const filtered = agreements
    .filter((a) => statusFilter === 'ALL' || a.status === statusFilter)
    .filter((a) => {
      const q = search.toLowerCase()
      return (
        !q ||
        a.agreementId?.toLowerCase().includes(q) ||
        a.productName?.toLowerCase().includes(q) ||
        a.seller?.toLowerCase().includes(q) ||
        a.buyer?.toLowerCase().includes(q)
      )
    })
    .sort((a, b) => {
      let av = a[sortField] ?? ''
      let bv = b[sortField] ?? ''
      if (typeof av === 'string') av = av.toLowerCase()
      if (typeof bv === 'string') bv = bv.toLowerCase()
      if (av < bv) return sortDir === 'asc' ? -1 : 1
      if (av > bv) return sortDir === 'asc' ? 1 : -1
      return 0
    })

  const toggleSort = (field) => {
    if (sortField === field) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    else { setSortField(field); setSortDir('asc') }
  }

  const SortIcon = ({ field }) =>
    sortField === field ? (
      <ArrowUpDown className="w-3 h-3 text-corda-600" />
    ) : (
      <ArrowUpDown className="w-3 h-3 text-slate-300" />
    )

  if (loading) return <LoadingSpinner message="Loading agreements from Corda ledger…" />
  if (error)
    return (
      <div className="card p-6 text-center">
        <p className="text-red-600 font-medium">Failed to load agreements</p>
        <p className="text-sm text-slate-500 mt-1">{error}</p>
      </div>
    )

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Agreements</h1>
          <p className="text-sm text-slate-500 mt-0.5">{agreements.length} total on ledger</p>
        </div>
        <Link to="/create" className="btn-primary">
          <FileText className="w-4 h-4" /> New Agreement
        </Link>
      </div>

      {/* Filters */}
      <div className="card p-4 flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            className="form-input pl-9"
            placeholder="Search by ID, product, seller, buyer…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400" />
          {STATUSES.map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors ${
                statusFilter === s
                  ? 'bg-corda-600 text-white border-corda-600'
                  : 'bg-white text-slate-600 border-slate-200 hover:border-corda-300'
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="card overflow-hidden">
        {filtered.length === 0 ? (
          <div className="py-12 text-center">
            <FileText className="w-10 h-10 text-slate-300 mx-auto mb-2" />
            <p className="text-slate-500 font-medium">No agreements found</p>
            <p className="text-sm text-slate-400 mt-1">
              {search || statusFilter !== 'ALL' ? 'Try adjusting the filters.' : 'Create your first agreement.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  {[
                    { label: 'Agreement ID', field: 'agreementId' },
                    { label: 'Product', field: 'productName' },
                    { label: 'Seller', field: 'seller' },
                    { label: 'Buyer', field: 'buyer' },
                    { label: 'Qty', field: 'quantity' },
                    { label: 'Price', field: 'totalPrice' },
                    { label: 'Delivery', field: 'deliveryDate' },
                    { label: 'Status', field: 'status' },
                  ].map(({ label, field }) => (
                    <th
                      key={field}
                      onClick={() => toggleSort(field)}
                      className="px-4 py-3 text-left font-medium text-slate-500 cursor-pointer hover:text-slate-700 select-none"
                    >
                      <span className="flex items-center gap-1">
                        {label} <SortIcon field={field} />
                      </span>
                    </th>
                  ))}
                  <th className="px-4 py-3 text-left font-medium text-slate-500">Tx ID</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((a) => (
                  <tr key={a.agreementId} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-slate-600">
                      <span className="flex items-center gap-1">
                        <Hash className="w-3 h-3 text-slate-400" />
                        {a.agreementId?.slice(0, 8)}…
                      </span>
                    </td>
                    <td className="px-4 py-3 font-medium text-slate-800 max-w-[140px] truncate">{a.productName}</td>
                    <td className="px-4 py-3 text-slate-600 max-w-[120px] truncate">{shortOrg(a.seller)}</td>
                    <td className="px-4 py-3 text-slate-600 max-w-[120px] truncate">{shortOrg(a.buyer)}</td>
                    <td className="px-4 py-3 text-slate-700">{a.quantity} {a.unit}</td>
                    <td className="px-4 py-3 text-slate-800 whitespace-nowrap">
                      {a.currency} {Number(a.totalPrice).toLocaleString('en-IN')}
                    </td>
                    <td className="px-4 py-3 text-slate-600 whitespace-nowrap">{a.deliveryDate}</td>
                    <td className="px-4 py-3"><StatusBadge status={a.status} /></td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-400 max-w-[100px] truncate">
                      {a.txId ? `${a.txId.slice(0, 8)}…` : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <Link
                        to={`/agreements/${a.agreementId}`}
                        className="text-corda-600 hover:text-corda-700 font-medium text-xs flex items-center gap-1 whitespace-nowrap"
                      >
                        View <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
