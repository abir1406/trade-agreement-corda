import { useState } from 'react'
import { useParty } from '../../context/PartyContext'
import { RefreshCw, ChevronDown, Building2, AlertCircle } from 'lucide-react'

export default function TopBar() {
  const { me, parties, switching, error, switchParty } = useParty()
  const [open, setOpen] = useState(false)

  const shortName = (name) => {
    if (!name) return '...'
    const match = name.match(/O=([^,]+)/)
    return match ? match[1] : name
  }

  const isSeller = me?.name?.includes('ABCElectronics')
  const roleColor = isSeller
    ? 'bg-blue-100 text-blue-800 border-blue-200'
    : 'bg-purple-100 text-purple-800 border-purple-200'
  const role = isSeller ? 'Seller' : 'Buyer'

  return (
    <header className="h-14 bg-white border-b border-slate-200 flex items-center justify-between px-6 shrink-0">
      <div className="flex items-center gap-2">
        <Building2 className="w-4 h-4 text-slate-400" />
        <span className="text-sm text-slate-500">Active identity:</span>
        <span className="text-sm font-semibold text-slate-800">{shortName(me?.name)}</span>
        {me && (
          <span className={`ml-2 text-xs px-2 py-0.5 rounded-full border font-medium ${roleColor}`}>
            {role}
          </span>
        )}
        {error && (
          <span className="flex items-center gap-1 text-xs text-red-600 ml-2">
            <AlertCircle className="w-3 h-3" /> Server unreachable
          </span>
        )}
      </div>

      {/* Party switcher */}
      <div className="relative">
        <button
          onClick={() => setOpen((o) => !o)}
          disabled={switching}
          className="flex items-center gap-2 text-sm font-medium text-slate-700 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 hover:bg-slate-100 transition-colors disabled:opacity-50"
        >
          {switching ? (
            <RefreshCw className="w-4 h-4 animate-spin" />
          ) : (
            <RefreshCw className="w-4 h-4 text-slate-400" />
          )}
          Switch Party
          <ChevronDown className="w-3 h-3 text-slate-400" />
        </button>

        {open && (
          <div className="absolute right-0 mt-1 w-64 bg-white border border-slate-200 rounded-xl shadow-lg z-50 overflow-hidden">
            <div className="px-3 py-2 bg-slate-50 border-b border-slate-100">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">Select Active Party</p>
            </div>
            {parties.map((p) => {
              const name = shortName(p.name)
              const active = me?.name === p.name
              return (
                <button
                  key={p.name}
                  onClick={() => { switchParty(p.name); setOpen(false) }}
                  className={`w-full text-left px-4 py-3 text-sm hover:bg-slate-50 transition-colors flex items-center justify-between ${
                    active ? 'text-corda-700 font-semibold bg-corda-50' : 'text-slate-700'
                  }`}
                >
                  <span>{name}</span>
                  {active && (
                    <span className="text-xs bg-corda-600 text-white px-2 py-0.5 rounded-full">Active</span>
                  )}
                </button>
              )
            })}
          </div>
        )}
      </div>
    </header>
  )
}
