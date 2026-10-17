import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { getAgreement, approveAgreement } from '../api/client'
import { useParty } from '../context/PartyContext'
import StatusBadge from '../components/StatusBadge'
import LoadingSpinner from '../components/LoadingSpinner'
import {
  ArrowLeft,
  CheckCircle,
  FileText,
  Calendar,
  Building,
  DollarSign,
  Package,
  Hash,
  AlertCircle,
  ShieldCheck,
  ExternalLink,
} from 'lucide-react'

export default function AgreementDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { me } = useParty()

  const [agreement, setAgreement] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [approving, setApproving] = useState(false)
  const [actionMsg, setActionMsg] = useState(null)

  useEffect(() => {
    loadAgreement()
  }, [id])

  const loadAgreement = async () => {
    setLoading(true)
    try {
      const data = await getAgreement(id)
      setAgreement(data)
      setError(null)
    } catch (err) {
      setError(err.message || 'Agreement not found')
    } finally {
      setLoading(false)
    }
  }

  const handleApprove = async () => {
    setApproving(true)
    setActionMsg(null)
    try {
      const res = await approveAgreement(id)
      setActionMsg({ type: 'success', text: `Agreement approved! New TxID: ${res.txId}` })
      await loadAgreement()
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Failed to approve agreement' })
    } finally {
      setApproving(false)
    }
  }

  const shortOrg = (name) => {
    if (!name) return '?'
    const m = name.match(/O=([^,]+)/)
    return m ? m[1] : name
  }

  if (loading) return <LoadingSpinner message="Querying vault state from Corda node..." />

  if (error || !agreement) {
    return (
      <div className="card p-8 text-center max-w-lg mx-auto">
        <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-3" />
        <h2 className="text-lg font-bold text-slate-800">Agreement Not Found</h2>
        <p className="text-sm text-slate-500 mt-1">{error || 'Could not find state in vault.'}</p>
        <Link to="/agreements" className="btn-secondary mt-4 inline-flex items-center gap-2">
          <ArrowLeft className="w-4 h-4" /> Back to Agreements
        </Link>
      </div>
    )
  }

  const isBuyer =
    me?.role === 'BUYER' ||
    me?.party === agreement.buyer ||
    me?.organisation === agreement.buyer ||
    me?.name === agreement.buyer ||
    me?.name?.includes(agreement.buyer)
  const canApprove = agreement.status === 'PENDING' && isBuyer

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/agreements')}
            className="p-2 hover:bg-slate-100 rounded-lg text-slate-500 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-slate-900">Agreement Details</h1>
              <StatusBadge status={agreement.status} />
            </div>
            <p className="text-xs font-mono text-slate-400 mt-0.5">ID: {agreement.agreementId}</p>
          </div>
        </div>

        {canApprove && (
          <button
            onClick={handleApprove}
            disabled={approving}
            className="btn-success"
          >
            <CheckCircle className="w-4 h-4" />
            {approving ? 'Signing & Notarizing...' : 'Approve Agreement'}
          </button>
        )}
        {agreement.status === 'PENDING' && !isBuyer && (
          <button
            onClick={() => switchParty(agreement.buyer)}
            className="btn-secondary text-xs py-2 px-3 flex items-center gap-1.5"
          >
            Switch to {shortOrg(agreement.buyer)} to Approve
          </button>
        )}
      </div>

      {actionMsg && (
        <div
          className={`p-4 rounded-xl border flex items-start gap-3 ${
            actionMsg.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
              : 'bg-red-50 border-red-200 text-red-800'
          }`}
        >
          {actionMsg.type === 'success' ? (
            <CheckCircle className="w-5 h-5 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          )}
          <p className="text-sm font-medium">{actionMsg.text}</p>
        </div>
      )}

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="card p-4">
          <div className="flex items-center gap-2 text-slate-400 text-xs font-semibold uppercase tracking-wider mb-2">
            <Package className="w-4 h-4" /> Product Details
          </div>
          <p className="text-lg font-bold text-slate-900">{agreement.productName}</p>
          <p className="text-sm text-slate-500 mt-1">
            {agreement.quantity} {agreement.unit}
          </p>
        </div>

        <div className="card p-4">
          <div className="flex items-center gap-2 text-slate-400 text-xs font-semibold uppercase tracking-wider mb-2">
            <DollarSign className="w-4 h-4" /> Financial Valuation
          </div>
          <p className="text-lg font-bold text-slate-900">
            {agreement.currency} {Number(agreement.totalPrice).toLocaleString('en-IN')}
          </p>
          <p className="text-sm text-slate-500 mt-1">
            Price per unit: {agreement.currency} {(agreement.totalPrice / agreement.quantity).toFixed(2)}
          </p>
        </div>

        <div className="card p-4">
          <div className="flex items-center gap-2 text-slate-400 text-xs font-semibold uppercase tracking-wider mb-2">
            <Calendar className="w-4 h-4" /> Schedule
          </div>
          <p className="text-lg font-bold text-slate-900">{agreement.deliveryDate}</p>
          <p className="text-sm text-slate-500 mt-1">Expected delivery timeline</p>
        </div>
      </div>

      {/* Main Details */}
      <div className="card p-6 space-y-6">
        <div>
          <h2 className="text-base font-semibold text-slate-800 mb-3 flex items-center gap-2">
            <Building className="w-4 h-4 text-slate-500" /> Counterparties & Legal Identities
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-4 rounded-lg bg-slate-50 border border-slate-200">
              <span className="text-xs font-semibold text-slate-500 uppercase">Seller Party (Originator)</span>
              <p className="text-base font-bold text-slate-900 mt-1">{shortOrg(agreement.seller)}</p>
              <p className="text-xs font-mono text-slate-500 break-all mt-1">{agreement.seller}</p>
            </div>
            <div className="p-4 rounded-lg bg-slate-50 border border-slate-200">
              <span className="text-xs font-semibold text-slate-500 uppercase">Buyer Party (Acceptor)</span>
              <p className="text-base font-bold text-slate-900 mt-1">{shortOrg(agreement.buyer)}</p>
              <p className="text-xs font-mono text-slate-500 break-all mt-1">{agreement.buyer}</p>
            </div>
          </div>
        </div>

        <div>
          <h2 className="text-base font-semibold text-slate-800 mb-2 flex items-center gap-2">
            <FileText className="w-4 h-4 text-slate-500" /> Description & Terms
          </h2>
          <div className="space-y-3">
            <div>
              <p className="text-xs text-slate-400 uppercase font-medium">Description</p>
              <p className="text-sm text-slate-700 mt-0.5 bg-slate-50 p-3 rounded-lg border border-slate-100">
                {agreement.description || 'No additional description specified.'}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-400 uppercase font-medium">Terms of Trade</p>
              <p className="text-sm text-slate-700 mt-0.5 bg-slate-50 p-3 rounded-lg border border-slate-100 whitespace-pre-wrap">
                {agreement.agreementTerms || 'Standard statutory trade terms.'}
              </p>
            </div>
          </div>
        </div>

        {/* Ledger Proof Section */}
        <div className="border-t border-slate-100 pt-5">
          <h2 className="text-base font-semibold text-slate-800 mb-3 flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-600" /> Corda Blockchain Ledger Proof
          </h2>
          <div className="space-y-2 text-xs">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded-lg bg-slate-50 border border-slate-200 gap-2">
              <span className="font-medium text-slate-600">Transaction ID (Hash):</span>
              <span className="font-mono text-slate-800 break-all">{agreement.txId || 'N/A'}</span>
            </div>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded-lg bg-slate-50 border border-slate-200 gap-2">
              <span className="font-medium text-slate-600">Linear ID:</span>
              <span className="font-mono text-slate-800 break-all">{agreement.linearId || agreement.agreementId}</span>
            </div>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded-lg bg-slate-50 border border-slate-200 gap-2">
              <span className="font-medium text-slate-600">State Index in Transaction:</span>
              <span className="font-mono text-slate-800">{agreement.outputIndex ?? 0}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
