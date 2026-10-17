import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { createAgreement, getParties } from '../api/client'
import { useParty } from '../context/PartyContext'
import { PlusCircle, AlertCircle, CheckCircle, ArrowLeft, ArrowRightLeft } from 'lucide-react'

export default function CreateAgreement() {
  const navigate = useNavigate()
  const { me, switchParty } = useParty()
  const [parties, setParties] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)

  const [form, setForm] = useState({
    productName: 'Laptops',
    description: 'High-performance enterprise laptops with 32GB RAM and 1TB SSD',
    quantity: 100,
    unit: 'units',
    totalPrice: 1000000,
    currency: 'INR',
    deliveryDate: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    buyer: '',
    agreementTerms: 'Standard commercial supply terms. Delivery within 14 business days upon buyer confirmation. Warranty valid for 3 years from dispatch date.',
  })

  useEffect(() => {
    getParties().then((list) => {
      setParties(list)
      // Pick the valid buyer party (not the seller ABCElectronics)
      const validBuyers = list.filter(
        (p) => !p.name.includes('ABCElectronics') && p.organisation !== 'ABCElectronics'
      )
      if (validBuyers.length > 0) {
        setForm((prev) => ({ ...prev, buyer: prev.buyer || validBuyers[0].name }))
      }
    }).catch(console.error)
  }, [me])

  const handleChange = (e) => {
    const { name, value, type } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'number' ? (value === '' ? '' : Number(value)) : value,
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)

    if (!form.productName.trim()) {
      setError('Product name is required.')
      return
    }
    if (form.quantity <= 0) {
      setError('Quantity must be greater than 0.')
      return
    }
    if (form.totalPrice <= 0) {
      setError('Total price must be greater than 0.')
      return
    }
    if (!form.buyer) {
      setError('Buyer must be selected.')
      return
    }
    if (!form.deliveryDate) {
      setError('Delivery date is required.')
      return
    }

    setSubmitting(true)
    try {
      const payload = {
        productName: form.productName.trim(),
        description: form.description?.trim() || '',
        quantity: Number(form.quantity),
        unit: form.unit?.trim() || 'units',
        totalPrice: Number(form.totalPrice),
        currency: form.currency || 'INR',
        deliveryDate: form.deliveryDate.includes('T') ? form.deliveryDate : `${form.deliveryDate}T00:00:00Z`,
        buyerParty: form.buyer,
        buyer: form.buyer,
      }
      const res = await createAgreement(payload)
      setSuccess(`Agreement created on Corda ledger! ID: ${res.agreementId}`)
      setTimeout(() => {
        navigate(`/agreements/${res.agreementId}`)
      }, 1500)
    } catch (err) {
      setError(err.message || 'Failed to create agreement on ledger.')
    } finally {
      setSubmitting(false)
    }
  }

  const shortOrg = (name) => {
    if (!name) return '?'
    const m = name.match(/O=([^,]+)/)
    return m ? m[1] : name
  }

  const isSeller =
    me?.role === 'SELLER' ||
    me?.party === 'ABCElectronics' ||
    me?.organisation === 'ABCElectronics' ||
    me?.name?.includes('ABCElectronics')

  const sellerDisplay = isSeller
    ? (me?.party || me?.organisation || shortOrg(me?.name))
    : 'ABCElectronics'

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate(-1)}
          className="p-2 hover:bg-slate-100 rounded-lg text-slate-500 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Create Trade Agreement</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Seller proposes a new trade agreement and records it on Corda ledger as PENDING
          </p>
        </div>
      </div>

      {!isSeller && (
        <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 flex items-center justify-between gap-3">
          <div className="flex items-start gap-3">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5 text-amber-600" />
            <div>
              <p className="font-semibold text-sm">Active Identity: Buyer (XYZ Retailers)</p>
              <p className="text-xs mt-0.5 text-amber-700">
                In this Corda network, commercial agreements are proposed by Seller (ABC Electronics).
              </p>
            </div>
          </div>
          <button
            onClick={() => switchParty('SELLER')}
            className="shrink-0 flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold bg-amber-600 hover:bg-amber-700 text-white rounded-lg transition-colors shadow-sm"
          >
            <ArrowRightLeft className="w-3.5 h-3.5" /> Switch to Seller
          </button>
        </div>
      )}

      {error && (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 flex items-start gap-3">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold text-sm">Error Submitting Agreement</p>
            <p className="text-xs mt-0.5">{error}</p>
          </div>
        </div>
      )}

      {success && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-700 flex items-start gap-3">
          <CheckCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold text-sm">Transaction Committed Successfully</p>
            <p className="text-xs mt-0.5">{success}</p>
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="card p-6 space-y-5">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="form-label">Current Seller (You)</label>
            <input
              type="text"
              disabled
              value={sellerDisplay}
              className="form-input bg-slate-50 text-slate-700 cursor-not-allowed font-medium"
            />
            <p className="text-xs text-slate-400 mt-1">Authorized Corda Seller Node (ABCElectronics)</p>
          </div>

          <div>
            <label className="form-label">Buyer Party *</label>
            <select
              name="buyer"
              value={form.buyer}
              onChange={handleChange}
              className="form-input"
              required
            >
              <option value="">Select Buyer Party</option>
              {parties
                .filter((p) => !p.name.includes('ABCElectronics') && p.organisation !== 'ABCElectronics')
                .map((p) => (
                  <option key={p.name} value={p.name}>
                    {p.organisation || shortOrg(p.name)} ({p.name})
                  </option>
                ))}
            </select>
            <p className="text-xs text-slate-400 mt-1">Authorized counterparty on Corda network</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="md:col-span-2">
            <label className="form-label">Product Name *</label>
            <input
              type="text"
              name="productName"
              value={form.productName}
              onChange={handleChange}
              placeholder="e.g. Laptops"
              className="form-input"
              required
            />
          </div>

          <div>
            <label className="form-label">Unit of Measurement</label>
            <input
              type="text"
              name="unit"
              value={form.unit}
              onChange={handleChange}
              placeholder="e.g. units, kg, pallets"
              className="form-input"
            />
          </div>
        </div>

        <div>
          <label className="form-label">Product Description</label>
          <textarea
            name="description"
            rows="2"
            value={form.description}
            onChange={handleChange}
            placeholder="Detailed product specifications..."
            className="form-input"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="form-label">Quantity *</label>
            <input
              type="number"
              min="1"
              name="quantity"
              value={form.quantity}
              onChange={handleChange}
              className="form-input"
              required
            />
          </div>

          <div>
            <label className="form-label">Total Price *</label>
            <input
              type="number"
              min="1"
              name="totalPrice"
              value={form.totalPrice}
              onChange={handleChange}
              className="form-input"
              required
            />
          </div>

          <div>
            <label className="form-label">Currency</label>
            <select
              name="currency"
              value={form.currency}
              onChange={handleChange}
              className="form-input"
            >
              <option value="INR">INR (₹)</option>
              <option value="USD">USD ($)</option>
              <option value="EUR">EUR (€)</option>
              <option value="GBP">GBP (£)</option>
            </select>
          </div>
        </div>

        <div>
          <label className="form-label">Delivery Date *</label>
          <input
            type="date"
            name="deliveryDate"
            value={form.deliveryDate}
            onChange={handleChange}
            className="form-input"
            required
          />
        </div>

        <div>
          <label className="form-label">Agreement Terms & Conditions</label>
          <textarea
            name="agreementTerms"
            rows="3"
            value={form.agreementTerms}
            onChange={handleChange}
            placeholder="Enter binding terms and delivery agreements..."
            className="form-input"
          />
        </div>

        <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={() => navigate('/agreements')}
            className="btn-secondary"
            disabled={submitting}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="btn-primary"
            disabled={submitting}
          >
            {submitting ? (
              <>Submitting to Corda Ledger...</>
            ) : (
              <>
                <PlusCircle className="w-4 h-4" /> Create & Record Agreement
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  )
}
