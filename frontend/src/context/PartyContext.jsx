import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { getMe, getParties, switchParty } from '../api/client'

const PartyContext = createContext(null)

export function PartyProvider({ children }) {
  const [me, setMe] = useState(null)
  const [parties, setParties] = useState([])
  const [loading, setLoading] = useState(true)
  const [switching, setSwitching] = useState(false)
  const [error, setError] = useState(null)

  const refresh = useCallback(async () => {
    try {
      const [meData, partiesData] = await Promise.all([getMe(), getParties()])
      setMe(meData)
      setParties(partiesData)
      setError(null)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh])

  const doSwitch = useCallback(async (partyName) => {
    setSwitching(true)
    try {
      await switchParty(partyName)
      await refresh()
    } finally {
      setSwitching(false)
    }
  }, [refresh])

  return (
    <PartyContext.Provider value={{ me, parties, loading, switching, error, refresh, switchParty: doSwitch }}>
      {children}
    </PartyContext.Provider>
  )
}

export const useParty = () => useContext(PartyContext)
