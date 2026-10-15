# Corda-Based Trade Agreement Management System

A production-ready distributed ledger application built with **Corda 4.12**, **Java 17**, **Spring Boot 2.7**, and **React 18 + Tailwind CSS**.

This project provides a complete, working decentralized trade agreement lifecycle between two autonomous counterparties:
- **Seller (`ABCElectronics`)**: Proposes commercial trade agreements and commits them to the Corda ledger in `PENDING` status.
- **Buyer (`XYZRetailers`)**: Reviews, verifies, and digitally co-signs the agreement, updating the ledger state to `APPROVED`.
- **Notary (`Notary Service`)**: Validates transaction inputs to ensure no double-spending and signs finality proofs.

---

## 🏗️ Architecture Overview

The system strictly adheres to the Corda UTXO model without any mock or simulated blockchain storage:

```
┌──────────────────────────────────────────────────────────┐
│                   React 18 Enterprise UI                 │
│         (Vite + Tailwind CSS + Lucide Icons)             │
└────────────────────────────┬─────────────────────────────┘
                             │ HTTP / JSON
┌────────────────────────────▼─────────────────────────────┐
│                 Spring Boot 2.7 REST Server              │
│       - NodeRPCConnection (MockNetwork Engine)           │
│       - Vault Query Service & Identity Switcher          │
└────────────────────────────┬─────────────────────────────┘
                             │ Corda Flow Calls
┌────────────────────────────▼─────────────────────────────┐
│             Corda 4.12 Distributed Ledger               │
│                                                          │
│  [Contracts & States]                                    │
│   ├── TradeAgreementState (LinearState, QueryableState)  │
│   └── TradeAgreementContract (Create & Approve Rules)    │
│                                                          │
│  [Workflows]                                             │
│   ├── CreateTradeAgreementFlow (Initiator & Responder)   │
│   └── ApproveTradeAgreementFlow (Initiator & Responder)  │
│                                                          │
│  [Ledger Participants]                                   │
│   ├── O=ABCElectronics, L=New York, C=US (Seller)        │
│   ├── O=XYZRetailers, L=Paris, C=FR (Buyer)              │
│   └── O=Notary Service, L=Zurich, C=CH (Notary)          │
└──────────────────────────────────────────────────────────┘
```

---

## 🌟 Key Features

1. **True Corda Distributed Ledger Transactions**:
   - Every trade agreement is backed by an immutable `TradeAgreementState` with unique Linear ID and cryptographically signed Transaction ID (`SecureHash`).
   - Approvals spend the unconsumed `PENDING` state and produce an `APPROVED` state notarized on the ledger.
2. **Strict Smart Contract Validation**:
   - `TradeAgreementContract` enforces non-empty names, positive quantities, valid delivery dates, and strict buyer authorization.
   - Unauthorized counterparties cannot approve agreements.
3. **No Docker / Complex Setup Required**:
   - The Spring Boot backend embeds an in-process, persistent Corda `MockNetwork` containing the Notary and both commercial nodes.
4. **Enterprise Dashboard**:
   - Live statistics directly queried from the Corda Vault.
   - Dynamic counterparty switcher to easily switch between Seller (`ABCElectronics`) and Buyer (`XYZRetailers`) view.
   - Filtering, search, sorting, and full cryptographic transaction proof view.

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java 17 JDK** (`java -version` should show Java 17)
- **Node.js 18+** & **npm**

### Step 1: Start the Corda Backend Server
From the project root:
```powershell
.\gradlew.bat :server:bootRun
```
*The Spring Boot server will initialize the Corda network, spin up the nodes, and start listening on `http://localhost:8080`.*

### Step 2: Start the Frontend UI
In a new terminal window:
```powershell
cd frontend
npm run dev
```
*Open `http://localhost:5173` in your browser.*

---

## 🧪 Running Automated Tests

All smart contracts and Corda workflows have comprehensive unit and integration test suites:

```powershell
# Test Smart Contract Verification rules (10 test cases)
.\gradlew.bat :contracts:test

# Test Corda Workflows and MockNetwork integration (3 multi-node test cases)
.\gradlew.bat :workflows:test
```

### Verified Test Cases:
- `TradeAgreementContractTests`:
  - `createTransactionMustHaveNoInputs`
  - `createTransactionMustHaveOneOutput`
  - `createTransactionSellerAndBuyerMustBeDifferent`
  - `createTransactionStatusMustBePending`
  - `createTransactionRequiresPositiveQuantity`
  - `createTransactionRequiresPositivePrice`
  - `approveTransactionMustHaveOneInputAndOneOutput`
  - `approveTransactionInputMustBePendingAndOutputApproved`
  - `approveTransactionTermsMustNotChange`
  - `approveTransactionMustBeSignedByBuyer`
- `TradeAgreementFlowTests`:
  - `successfulAgreementCreationFlow` (notarized on ledger)
  - `successfulAgreementApprovalFlow` (transitioned to APPROVED)
  - `unauthorizedPartyCannotApprove` (fails contract verification as required)

---

## 📡 REST API Reference

| Endpoint | Method | Description |
|---|---|---|
| `/api/me` | GET | Returns the currently active Corda party |
| `/api/parties` | GET | Lists available counterparties on the network |
| `/api/switch-party` | POST | Switches active persona (`ABCElectronics` or `XYZRetailers`) |
| `/api/stats` | GET | Vault aggregated metrics (total, pending, approved, value) |
| `/api/agreements` | GET | List all agreements recorded in the node's vault |
| `/api/agreements/{id}` | GET | Fetch details and cryptographic proof of a specific agreement |
| `/api/agreements` | POST | Seller creates and records a new trade agreement |
| `/api/agreements/{id}/approve` | POST | Buyer digitally approves and notarizes agreement |

---

## 🎓 College Mini-Project Presentation Points

- **Why Corda over Public Blockchains (Ethereum / Bitcoin)**:
  - Enterprise privacy: Agreements are shared strictly on a **need-to-know basis** between Seller, Buyer, and Notary.
  - Finality: Instant transaction finality without probabilistic proof-of-work or gas volatility.
- **UTXO (Unspent Transaction Output) Concept**:
  - Explains how state evolution occurs: `PENDING` state is consumed as an input, producing an immutable `APPROVED` state.
- **Quasar & Fibers**:
  - Demonstrates understanding of Corda flow checkpoints, fiber suspensions, and asynchronous messaging across distributed nodes.
