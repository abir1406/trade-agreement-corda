import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getAgreements, approveAgreement } from '../api/client'
import { useParty } from '../context/PartyContext'
import StatusBadge from '../components/StatusBadge'
import LoadingSpinner from '../components/LoadingSpinner'
import {
  CheckSquare,
  AlertCircle,
  CheckCircle2,
  Building2,
  Clock,
  ArrowRight,
} from 'lucide-react'

export default function ApprovePage() {
  const { me, switchParty } = useParty()
  const [agreements, setAgreements] = useState([])
  const [loading, setLoading] = useState(true)
  const [approvingId, setApprovingId] = useState(null)
  const [notification, setNotification] = useState(null)

  useEffect(() => {
    loadPending()
  }, [me])

  const loadPending = async () => {
    setLoading(false)
    try {
      const all = await getAgreements()
      setAgreements(all.filter((a) => a.status === 'PENDING'))
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleApprove = async (agreementId) => {
    setApprovingId(agreementId)
    setNotification(null)
    try {
      const res = await approveAgreement(agreementId)
      setNotification({
        type: 'success',
        text: `Agreement ${agreementId.slice(0, 8)}... approved successfully! TxID: ${res.txId?.slice(0, 12)}...`,
      })
      await loadPending()
    } catch (err) {
      setNotification({
        type: 'error',
        text: err.message || 'Approval failed. Check node permissions or agreement status.',
      })
    } finally {
      setApprovingId(null)
    }
  }

  const shortOrg = (name) => {
    if (!name) return '?'
    const m = name.match(/O=([^,]+)/)
    return m ? m[1] : name
  }

  const isMeBuyer = (buyerOrgOrName) => {
    if (!buyerOrgOrName || !me) return false
    return (
      buyerOrgOrName === me.party ||
      buyerOrgOrName === me.organisation ||
      buyerOrgOrName === me.name ||
      me.name?.includes(buyerOrgOrName) ||
      (me.role === 'BUYER' && (buyerOrgOrName === 'XYZRetailers' || buyerOrgOrName.includes('XYZ')))
    )
  }

  const myPending = agreements.filter((a) => isMeBuyer(a.buyer))
  const otherPending = agreements.filter((a) => !isMeBuyer(a.buyer))

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Approve Agreements</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Review and digitally sign trade agreements pending your buyer authorization
          </p>
        </div>
      </div>

      {notification && (
        <div
          className={`p-4 rounded-xl border flex items-start gap-3 ${
            notification.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
              : 'bg-red-50 border-red-200 text-red-800'
          }`}
        >
          {notification.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          )}
          <p className="text-sm font-medium">{notification.text}</p>
        </div>
      )}

      {/* Agreements assigned to current active party */}
      <div className="space-y-4">
        <h2 className="text-base font-semibold text-slate-800 flex items-center gap-2">
          <Clock className="w-4 h-4 text-amber-500" />
          Pending Approvals for You ({me?.party || me?.organisation || shortOrg(me?.name)})
        </h2>

        {myPending.length === 0 ? (
          <div className="card p-8 text-center">
            <CheckSquare className="w-10 h-10 text-slate-300 mx-auto mb-2" />
            <p className="text-slate-600 font-medium">No agreements awaiting your approval</p>
            <p className="text-xs text-slate-400 mt-1">
              Either all agreements are approved, or new proposals were sent to another counterparty.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {myPending.map((a) => (
              <div key={a.agreementId} className="card p-5 flex flex-col justify-between">
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <h3 className="font-bold text-slate-900">{a.productName}</h3>
                      <p className="text-xs font-mono text-slate-400 mt-0.5">ID: {a.agreementId?.slice(0, 8)}...</p>
                    </div>
                    <StatusBadge status={a.status} />
                  </div>

                  <p className="text-xs text-slate-600 mt-3 line-clamp-2">
                    {a.description || 'No description'}
                  </p>

                  <div className="mt-4 pt-3 border-t border-slate-100 grid grid-cols-2 gap-2 text-xs">
                    <div>
                      <span className="text-slate-400 block">Seller:</span>
                      <span className="font-medium text-slate-700">{shortOrg(a.seller)}</span>
                    </div>
                    <div>
                      <span className="text-slate-400 block">Total Value:</span>
                      <span className="font-bold text-slate-900">
                        {a.currency} {Number(a.totalPrice).toLocaleString('en-IN')}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block">Quantity:</span>
                      <span className="font-medium text-slate-700">
                        {a.quantity} {a.unit}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block">Delivery Date:</span>
                      <span className="font-medium text-slate-700">{a.deliveryDate}</span>
                    </div>
                  </div>
                </div>

                <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                  <Link
                    to={`/agreements/${a.agreementId}`}
                    className="text-xs text-slate-500 hover:text-slate-800 flex items-center gap-1"
                  >
                    Details <ArrowRight className="w-3 h-3" />
                  </Link>

                  <button
                    onClick={() => handleApprove(a.agreementId)}
                    disabled={approvingId === a.agreementId}
                    className="btn-success text-xs py-1.5 px-3"
                  >
                    <CheckSquare className="w-3.5 h-3.5" />
                    {approvingId === a.agreementId ? 'Approving...' : 'Approve on Corda'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Pending for other parties */}
      {otherPending.length > 0 && (
        <div className="space-y-3 pt-6 border-t border-slate-200">
          <h2 className="text-sm font-semibold text-slate-600 flex items-center gap-2">
            <Building2 className="w-4 h-4 text-slate-400" />
            Pending for Other Counterparties
          </h2>
          <p className="text-xs text-slate-500">
            Corda contract rules enforce that only the designated Buyer can approve an agreement. You can switch to their identity to simulate approval.
          </p>

          <div className="card overflow-hidden">
            <table className="w-full text-xs">
              <thead className="bg-slate-50 border-b border-slate-100">
                <tr>
                  <th className="px-4 py-2.5 text-left text-slate-500 font-medium">Agreement</th>
                  <th className="px-4 py-2.5 text-left text-slate-500 font-medium">Required Buyer</th>
                  <th className="px-4 py-2.5 text-left text-slate-500 font-medium">Value</th>
                  <th className="px-4 py-2.5 text-right text-slate-500 font-medium">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {otherPending.map((a) => (
                  <tr key={a.agreementId} className="hover:bg-slate-50">
                    <td className="px-4 py-2.5 font-medium text-slate-800">{a.productName}</td>
                    <td className="px-4 py-2.5 text-slate-600">{shortOrg(a.buyer)}</td>
                    <td className="px-4 py-2.5 text-slate-700">
                      {a.currency} {Number(a.totalPrice).toLocaleString('en-IN')}
                    </td>
                    <td className="px-4 py-2.5 text-right">
                      <button
                        onClick={() => switchParty(a.buyer)}
                        className="text-corda-600 hover:text-corda-700 font-semibold"
                      >
                        Switch to {shortOrg(a.buyer)}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
