const BASE_URL = '/api'

async function request(method, path, body) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body !== undefined) {
    opts.body = JSON.stringify(body)
  }
  const res = await fetch(`${BASE_URL}${path}`, opts)
  let data
  try {
    data = await res.json()
  } catch {
    data = null
  }
  if (!res.ok) {
    const msg = data?.message || data?.error || `HTTP ${res.status}`
    throw new Error(msg)
  }
  return data
}

// ── Party & Identity ──────────────────────────────────────────────────────────
export const getMe = () => request('GET', '/me')
export const getParties = () => request('GET', '/parties')
export const switchParty = (partyNameOrRole) => {
  const role =
    typeof partyNameOrRole === 'string' &&
    (partyNameOrRole.toUpperCase().includes('BUYER') ||
      partyNameOrRole.includes('XYZ') ||
      partyNameOrRole.includes('Paris'))
      ? 'BUYER'
      : 'SELLER'
  return request('POST', '/switch-party', { partyName: partyNameOrRole, role })
}

// ── Stats ─────────────────────────────────────────────────────────────────────
export const getStats = () => request('GET', '/stats')

// ── Agreements ────────────────────────────────────────────────────────────────
export const getAgreements = () => request('GET', '/agreements')
export const getAgreement = (id) => request('GET', `/agreements/${id}`)
export const createAgreement = (payload) =>
  request('POST', '/agreements', payload)
export const approveAgreement = (id) =>
  request('POST', `/agreements/${id}/approve`)
