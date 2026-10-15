package net.corda.tradeagreement.flows;

import java.util.Arrays;
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
import net.corda.tradeagreement.states.TradeAgreementState;
import net.corda.tradeagreement.states.TradeAgreementStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class TradeAgreementFlowTests {

    private MockNetwork network;
    private StartedMockNode sellerNode;
    private StartedMockNode buyerNode;
    private Party sellerParty;
    private Party buyerParty;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters(Arrays.asList(
                TestCordapp.findCordapp("net.corda.tradeagreement.contracts"),
                TestCordapp.findCordapp("net.corda.tradeagreement.flows")
        )));

        sellerNode = network.createPartyNode(new CordaX500Name("ABCElectronics", "New York", "US"));
        buyerNode = network.createPartyNode(new CordaX500Name("XYZRetailers", "Paris", "FR"));

        sellerParty = sellerNode.getInfo().getLegalIdentities().get(0);
        buyerParty = buyerNode.getInfo().getLegalIdentities().get(0);

        network.runNetwork();
    }

    @After
    public void tearDown() {
        if (network != null) {
            network.stopNodes();
        }
    }

    @Test
    public void testCreateTradeAgreementFlowRecordsInBothVaults() throws Exception {
        Instant deliveryDate = Instant.now().plus(45, ChronoUnit.DAYS);
        CreateTradeAgreementFlow flow = new CreateTradeAgreementFlow(
                "AGR-2026-101",
                buyerParty,
                "Laptops",
                "100 enterprise laptops with 32GB RAM",
                100,
                "units",
                new BigDecimal("1000000.00"),
                "INR",
                deliveryDate
        );

        CordaFuture<SignedTransaction> future = sellerNode.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        assertNotNull("Signed transaction should not be null", signedTx);

        // Check seller's vault
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<TradeAgreementState>> sellerStates = sellerNode.getServices().getVaultService()
                .queryBy(TradeAgreementState.class, criteria).getStates();
        assertEquals(1, sellerStates.size());

        TradeAgreementState sellerRecorded = sellerStates.get(0).getState().getData();
        assertEquals("AGR-2026-101", sellerRecorded.getAgreementId());
        assertEquals(TradeAgreementStatus.PENDING, sellerRecorded.getStatus());
        assertEquals(sellerParty, sellerRecorded.getSeller());
        assertEquals(buyerParty, sellerRecorded.getBuyer());

        // Check buyer's vault
        List<StateAndRef<TradeAgreementState>> buyerStates = buyerNode.getServices().getVaultService()
                .queryBy(TradeAgreementState.class, criteria).getStates();
        assertEquals(1, buyerStates.size());

        TradeAgreementState buyerRecorded = buyerStates.get(0).getState().getData();
        assertEquals("AGR-2026-101", buyerRecorded.getAgreementId());
        assertEquals(TradeAgreementStatus.PENDING, buyerRecorded.getStatus());
        assertEquals(sellerParty, buyerRecorded.getSeller());
        assertEquals(buyerParty, buyerRecorded.getBuyer());
    }

    @Test
    public void testApproveTradeAgreementFlowUpdatesStateToApprovedInBothVaults() throws Exception {
        // Step 1: Seller creates agreement
        Instant deliveryDate = Instant.now().plus(45, ChronoUnit.DAYS);
        CreateTradeAgreementFlow createFlow = new CreateTradeAgreementFlow(
                "AGR-2026-202",
                buyerParty,
                "Laptops",
                "High quality laptops",
                100,
                "units",
                new BigDecimal("1000000.00"),
                "INR",
                deliveryDate
        );

        CordaFuture<SignedTransaction> createFuture = sellerNode.startFlow(createFlow);
        network.runNetwork();
        createFuture.get();

        // Step 2: Buyer approves agreement
        ApproveTradeAgreementFlow approveFlow = new ApproveTradeAgreementFlow("AGR-2026-202");
        CordaFuture<SignedTransaction> approveFuture = buyerNode.startFlow(approveFlow);
        network.runNetwork();
        SignedTransaction approveTx = approveFuture.get();

        assertNotNull("Approval transaction should not be null", approveTx);

        // Step 3: Verify unconsumed state in both vaults is APPROVED
        QueryCriteria unconsumedCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        List<StateAndRef<TradeAgreementState>> buyerStates = buyerNode.getServices().getVaultService()
                .queryBy(TradeAgreementState.class, unconsumedCriteria).getStates();
        assertEquals(1, buyerStates.size());
        assertEquals(TradeAgreementStatus.APPROVED, buyerStates.get(0).getState().getData().getStatus());

        List<StateAndRef<TradeAgreementState>> sellerStates = sellerNode.getServices().getVaultService()
                .queryBy(TradeAgreementState.class, unconsumedCriteria).getStates();
        assertEquals(1, sellerStates.size());
        assertEquals(TradeAgreementStatus.APPROVED, sellerStates.get(0).getState().getData().getStatus());

        // Step 4: Verify consumed states (the previous PENDING state is consumed)
        QueryCriteria consumedCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED);
        List<StateAndRef<TradeAgreementState>> consumedStates = buyerNode.getServices().getVaultService()
                .queryBy(TradeAgreementState.class, consumedCriteria).getStates();
        assertEquals(1, consumedStates.size());
        assertEquals(TradeAgreementStatus.PENDING, consumedStates.get(0).getState().getData().getStatus());
    }

    @Test
    public void testSellerCannotApproveOwnAgreement() throws Exception {
        // Create agreement
        Instant deliveryDate = Instant.now().plus(30, ChronoUnit.DAYS);
        CreateTradeAgreementFlow createFlow = new CreateTradeAgreementFlow(
                "AGR-2026-303",
                buyerParty,
                "Laptops",
                "Test unauthorized approval",
                50,
                "units",
                new BigDecimal("500000.00"),
                "INR",
                deliveryDate
        );
        CordaFuture<SignedTransaction> createFuture = sellerNode.startFlow(createFlow);
        network.runNetwork();
        createFuture.get();

        // Seller attempts to approve instead of Buyer -> should fail
        ApproveTradeAgreementFlow unauthorizedApproval = new ApproveTradeAgreementFlow("AGR-2026-303");
        CordaFuture<SignedTransaction> failFuture = sellerNode.startFlow(unauthorizedApproval);
        network.runNetwork();

        try {
            failFuture.get();
            fail("Expected ExecutionException caused by FlowException");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof FlowException);
            assertTrue(e.getCause().getMessage().contains("Unauthorized: Only the designated buyer"));
        }
    }
}
