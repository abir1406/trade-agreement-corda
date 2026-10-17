package net.corda.tradeagreement.server;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import net.corda.tradeagreement.flows.ApproveTradeAgreementFlow;
import net.corda.tradeagreement.flows.CreateTradeAgreementFlow;
import net.corda.tradeagreement.server.dto.AgreementResponse;
import net.corda.tradeagreement.server.dto.CreateAgreementRequest;
import net.corda.tradeagreement.server.dto.DashboardStatsResponse;
import net.corda.tradeagreement.server.dto.PartyResponse;
import net.corda.tradeagreement.states.TradeAgreementState;
import net.corda.tradeagreement.states.TradeAgreementStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service managing access to the Corda ledger.
 * Initializes and manages an active Corda network instance with Notary,
 * ABC Electronics (Seller), and XYZ Retailers (Buyer), executing real Corda transactions.
 */
@Service
public class NodeRPCConnection {

    private static final Logger log = LoggerFactory.getLogger(NodeRPCConnection.class);

    private MockNetwork network;
    private StartedMockNode sellerNode;
    private StartedMockNode buyerNode;
    private Party sellerParty;
    private Party buyerParty;

    // Active party for UI simulation: "SELLER" or "BUYER"
    private String activeRole = "SELLER";

    @PostConstruct
    public void init() {
        log.info("Initializing Corda Distributed Ledger Network...");
        try {
            network = new MockNetwork(new MockNetworkParameters(Arrays.asList(
                    TestCordapp.findCordapp("net.corda.tradeagreement.contracts"),
                    TestCordapp.findCordapp("net.corda.tradeagreement.flows")
            )));

            sellerNode = network.createPartyNode(new CordaX500Name("ABCElectronics", "New York", "US"));
            buyerNode = network.createPartyNode(new CordaX500Name("XYZRetailers", "Paris", "FR"));

            sellerParty = sellerNode.getInfo().getLegalIdentities().get(0);
            buyerParty = buyerNode.getInfo().getLegalIdentities().get(0);

            network.runNetwork();
            log.info("Corda Network initialized successfully with Seller [{}] and Buyer [{}]",
                    sellerParty.getName().getOrganisation(), buyerParty.getName().getOrganisation());
        } catch (Exception e) {
            log.error("Failed to initialize Corda network", e);
            throw new RuntimeException("Corda network initialization failed", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (network != null) {
            log.info("Stopping Corda network nodes...");
            network.stopNodes();
        }
    }

    public synchronized StartedMockNode getActiveNode() {
        return "BUYER".equalsIgnoreCase(activeRole) ? buyerNode : sellerNode;
    }

    public synchronized Party getActiveParty() {
        return "BUYER".equalsIgnoreCase(activeRole) ? buyerParty : sellerParty;
    }

    public synchronized String getActiveRole() {
        return activeRole;
    }

    public synchronized void setActiveRole(String roleOrParty) {
        if (roleOrParty != null) {
            String upper = roleOrParty.toUpperCase();
            if (upper.contains("BUYER") || upper.contains("XYZ") || upper.contains("PARIS")) {
                this.activeRole = "BUYER";
            } else {
                this.activeRole = "SELLER";
            }
        } else {
            this.activeRole = "SELLER";
        }
        log.info("Switched active Corda party context to: {} ({})",
                getActiveParty().getName().getOrganisation(), this.activeRole);
    }

    public List<PartyResponse> getAvailableParties() {
        List<PartyResponse> list = new ArrayList<>();
        list.add(new PartyResponse(
                sellerParty.getName().toString(),
                sellerParty.getName().getOrganisation(),
                sellerParty.getName().getLocality(),
                sellerParty.getName().getCountry()
        ));
        list.add(new PartyResponse(
                buyerParty.getName().toString(),
                buyerParty.getName().getOrganisation(),
                buyerParty.getName().getLocality(),
                buyerParty.getName().getCountry()
        ));
        return list;
    }

    public synchronized AgreementResponse createAgreement(CreateAgreementRequest request) throws Exception {
        String buyerPartyName = request.getBuyerParty();
        if (buyerPartyName == null || buyerPartyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Buyer party name is required.");
        }

        // Validate and resolve buyer party from configured network counterparties
        Party buyer = null;
        if (buyerParty.getName().toString().equalsIgnoreCase(buyerPartyName.trim()) ||
                buyerParty.getName().getOrganisation().equalsIgnoreCase(buyerPartyName.trim())) {
            buyer = buyerParty;
        } else if (sellerParty.getName().toString().equalsIgnoreCase(buyerPartyName.trim()) ||
                sellerParty.getName().getOrganisation().equalsIgnoreCase(buyerPartyName.trim())) {
            buyer = sellerParty;
        } else {
            throw new IllegalArgumentException("Selected buyer party '" + buyerPartyName +
                    "' does not exist on the configured Corda network. Configured parties: " +
                    sellerParty.getName().getOrganisation() + ", " + buyerParty.getName().getOrganisation());
        }

        if (sellerParty.equals(buyer)) {
            throw new IllegalArgumentException("Seller and Buyer must be distinct Corda network identities. Cannot create an agreement with yourself.");
        }

        String agreementId = request.getAgreementId();
        if (agreementId == null || agreementId.trim().isEmpty()) {
            agreementId = "AGR-" + System.currentTimeMillis();
        }

        CreateTradeAgreementFlow flow = new CreateTradeAgreementFlow(
                agreementId,
                buyer,
                request.getProductName(),
                request.getDescription(),
                request.getQuantity(),
                request.getUnit() != null ? request.getUnit() : "units",
                request.getTotalPrice(),
                request.getCurrency() != null ? request.getCurrency() : "INR",
                request.getDeliveryDate()
        );

        log.info("Executing CreateTradeAgreementFlow for agreementId: {} with Seller [{}] and Buyer [{}]",
                agreementId, sellerParty.getName().getOrganisation(), buyer.getName().getOrganisation());
        CordaFuture<SignedTransaction> future = sellerNode.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        TradeAgreementState state = signedTx.getCoreTransaction().outputsOfType(TradeAgreementState.class).get(0);
        String txId = signedTx.getId().toString();
        String notary = signedTx.getNotary() != null ? signedTx.getNotary().getName().getOrganisation() : "Notary Service";

        log.info("Trade Agreement [{}] recorded on Corda ledger. TxId: {}", agreementId, txId);

        return toAgreementResponse(state, txId, notary, false);
    }

    public synchronized AgreementResponse approveAgreement(String agreementId) throws Exception {
        log.info("Executing ApproveTradeAgreementFlow for agreementId: {}", agreementId);

        ApproveTradeAgreementFlow flow = new ApproveTradeAgreementFlow(agreementId);
        CordaFuture<SignedTransaction> future = buyerNode.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        TradeAgreementState state = signedTx.getCoreTransaction().outputsOfType(TradeAgreementState.class).get(0);
        String txId = signedTx.getId().toString();
        String notary = signedTx.getNotary() != null ? signedTx.getNotary().getName().getOrganisation() : "Notary Service";

        log.info("Trade Agreement [{}] APPROVED on Corda ledger. New TxId: {}", agreementId, txId);

        return toAgreementResponse(state, txId, notary, false);
    }

    public List<AgreementResponse> getAllAgreements() {
        StartedMockNode node = getActiveNode();
        QueryCriteria unconsumedCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<TradeAgreementState>> unconsumed = node.getServices().getVaultService()
                .queryBy(TradeAgreementState.class, unconsumedCriteria).getStates();

        List<AgreementResponse> responses = new ArrayList<>();
        for (StateAndRef<TradeAgreementState> sar : unconsumed) {
            TradeAgreementState state = sar.getState().getData();
            String txId = sar.getRef().getTxhash().toString();
            String notary = sar.getState().getNotary().getName().getOrganisation();
            responses.add(toAgreementResponse(state, txId, notary, false));
        }

        // Sort by agreement ID descending
        responses.sort((a, b) -> b.getAgreementId().compareTo(a.getAgreementId()));
        return responses;
    }

    public AgreementResponse getAgreementById(String agreementId) {
        StartedMockNode node = getActiveNode();
        QueryCriteria allCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        List<StateAndRef<TradeAgreementState>> states = node.getServices().getVaultService()
                .queryBy(TradeAgreementState.class, allCriteria).getStates();

        StateAndRef<TradeAgreementState> found = null;
        boolean isConsumed = false;

        // Prefer UNCONSUMED
        for (StateAndRef<TradeAgreementState> sar : states) {
            if (sar.getState().getData().getAgreementId().equalsIgnoreCase(agreementId)) {
                found = sar;
                if (sar.getState().getData().getStatus() == TradeAgreementStatus.APPROVED) {
                    break;
                }
            }
        }

        if (found == null) {
            return null;
        }

        TradeAgreementState state = found.getState().getData();
        String txId = found.getRef().getTxhash().toString();
        String notary = found.getState().getNotary().getName().getOrganisation();
        return toAgreementResponse(state, txId, notary, isConsumed);
    }

    public DashboardStatsResponse getStats() {
        List<AgreementResponse> all = getAllAgreements();
        long total = all.size();
        long pending = all.stream().filter(a -> "PENDING".equalsIgnoreCase(a.getStatus())).count();
        long approved = all.stream().filter(a -> "APPROVED".equalsIgnoreCase(a.getStatus())).count();

        // Total recorded ledger transactions in node's storage
        StartedMockNode node = getActiveNode();
        long totalTxs = node.getServices().getValidatedTransactions().track().getSnapshot().size();
        if (totalTxs == 0) {
            totalTxs = total;
        }

        return new DashboardStatsResponse(
                total,
                pending,
                approved,
                totalTxs,
                getActiveParty().getName().getOrganisation(),
                getActiveParty().getName().toString(),
                getActiveRole()
        );
    }

    private AgreementResponse toAgreementResponse(TradeAgreementState state, String txId, String notary, boolean consumed) {
        return new AgreementResponse(
                state.getAgreementId(),
                state.getLinearId().getId().toString(),
                state.getSeller().getName().getOrganisation(),
                state.getBuyer().getName().getOrganisation(),
                state.getProductName(),
                state.getDescription(),
                state.getQuantity(),
                state.getUnit(),
                state.getTotalPrice(),
                state.getCurrency(),
                state.getDeliveryDate(),
                state.getStatus().name(),
                txId,
                notary,
                consumed
        );
    }
}
