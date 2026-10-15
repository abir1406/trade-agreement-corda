import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { PartyProvider } from './context/PartyContext'
import Layout from './components/Layout/Layout'
import Dashboard from './pages/Dashboard'
import AgreementList from './pages/AgreementList'
import CreateAgreement from './pages/CreateAgreement'
import AgreementDetail from './pages/AgreementDetail'
import ApprovePage from './pages/ApprovePage'

export default function App() {
  return (
    <PartyProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<Dashboard />} />
            <Route path="agreements" element={<AgreementList />} />
            <Route path="agreements/:id" element={<AgreementDetail />} />
            <Route path="create" element={<CreateAgreement />} />
            <Route path="approve" element={<ApprovePage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </PartyProvider>
  )
}
