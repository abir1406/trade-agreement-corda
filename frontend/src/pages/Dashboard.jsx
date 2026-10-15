import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getStats, getAgreements } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import LoadingSpinner from '../components/LoadingSpinner'
import {
  FileText,
  Clock,
  CheckCircle2,
  TrendingUp,
  PlusCircle,
  ArrowRight,
  Hash,
} from 'lucide-react'

function StatCard({ icon: Icon, label, value, color }) {
  return (
    <div className="card p-5 flex items-center gap-4">
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${color}`}>
        <Icon className="w-6 h-6" />
      </div>
      <div>
        <p className="text-sm text-slate-500 font-medium">{label}</p>
        <p className="text-2xl font-bold text-slate-900">{value ?? '—'}</p>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const [stats, setStats] = useState(null)
  const [recent, setRecent] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    async function load() {
      try {
        const [s, all] = await Promise.all([getStats(), getAgreements()])
        setStats(s)
        setRecent([...all].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)).slice(0, 5))
      } catch (e) {
        setError(e.message)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) return <LoadingSpinner message="Loading dashboard…" />
  if (error)
    return (
      <div className="card p-6 text-center">
        <p className="text-red-600 font-medium">Failed to load dashboard</p>
        <p className="text-sm text-slate-500 mt-1">{error}</p>
        <p className="text-xs text-slate-400 mt-2">Make sure the Corda server is running on port 8080.</p>
      </div>
    )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Dashboard</h1>
          <p className="text-sm text-slate-500 mt-0.5">Trade Agreement Management · Powered by Corda 4.12</p>
        </div>
        <Link to="/create" className="btn-primary">
          <PlusCircle className="w-4 h-4" />
          New Agreement
        </Link>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={FileText}
          label="Total Agreements"
          value={stats?.total}
          color="bg-blue-100 text-blue-600"
        />
        <StatCard
          icon={Clock}
          label="Pending"
          value={stats?.pending}
          color="bg-amber-100 text-amber-600"
        />
        <StatCard
          icon={CheckCircle2}
          label="Approved"
          value={stats?.approved}
          color="bg-emerald-100 text-emerald-600"
        />
        <StatCard
          icon={TrendingUp}
          label="Total Value (₹)"
          value={stats?.totalValue != null ? Number(stats.totalValue).toLocaleString('en-IN') : '—'}
          color="bg-purple-100 text-purple-600"
        />
      </div>

      {/* Recent Agreements */}
      <div className="card overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h2 className="font-semibold text-slate-800">Recent Agreements</h2>
          <Link to="/agreements" className="text-sm text-corda-600 hover:text-corda-700 flex items-center gap-1">
            View all <ArrowRight className="w-3 h-3" />
          </Link>
        </div>

        {recent.length === 0 ? (
          <div className="px-6 py-10 text-center">
            <FileText className="w-10 h-10 text-slate-300 mx-auto mb-2" />
            <p className="text-slate-500 font-medium">No agreements yet</p>
            <p className="text-sm text-slate-400 mt-1">Create your first trade agreement to get started.</p>
            <Link to="/create" className="btn-primary mt-4 inline-flex">
              <PlusCircle className="w-4 h-4" /> Create Agreement
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="px-6 py-3 text-left font-medium text-slate-500">ID</th>
                  <th className="px-6 py-3 text-left font-medium text-slate-500">Product</th>
                  <th className="px-6 py-3 text-left font-medium text-slate-500">Seller → Buyer</th>
                  <th className="px-6 py-3 text-left font-medium text-slate-500">Price</th>
                  <th className="px-6 py-3 text-left font-medium text-slate-500">Status</th>
                  <th className="px-6 py-3 text-left font-medium text-slate-500"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {recent.map((a) => (
                  <tr key={a.agreementId} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-3 font-mono text-xs text-slate-600 flex items-center gap-1">
                      <Hash className="w-3 h-3 text-slate-400" />{a.agreementId?.slice(0, 8)}…
                    </td>
                    <td className="px-6 py-3 font-medium text-slate-800">{a.productName}</td>
                    <td className="px-6 py-3 text-slate-600 text-xs">
                      {shortOrg(a.seller)} → {shortOrg(a.buyer)}
                    </td>
                    <td className="px-6 py-3 text-slate-800">
                      {a.currency} {Number(a.totalPrice).toLocaleString('en-IN')}
                    </td>
                    <td className="px-6 py-3"><StatusBadge status={a.status} /></td>
                    <td className="px-6 py-3">
                      <Link
                        to={`/agreements/${a.agreementId}`}
                        className="text-corda-600 hover:text-corda-700 font-medium text-xs flex items-center gap-1"
                      >
                        Details <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Corda Info */}
      <div className="card p-4 flex items-center gap-3 bg-gradient-to-r from-corda-50 to-blue-50 border-corda-100">
        <div className="w-8 h-8 rounded-lg bg-corda-600 flex items-center justify-center shrink-0">
          <Hash className="w-4 h-4 text-white" />
        </div>
        <div className="text-sm text-slate-600">
          All data is retrieved from the{' '}
          <span className="font-semibold text-corda-700">Corda distributed ledger</span> via the MockNetwork
          server. No data is hardcoded or simulated.
        </div>
      </div>
    </div>
  )
}

function shortOrg(name) {
  if (!name) return '?'
  const m = name.match(/O=([^,]+)/)
  return m ? m[1] : name
}
