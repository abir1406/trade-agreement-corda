package net.corda.tradeagreement.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.tradeagreement.contracts.TradeAgreementContract;
import net.corda.tradeagreement.states.TradeAgreementState;
import net.corda.tradeagreement.states.TradeAgreementStatus;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Initiating flow executed by the Buyer to approve an existing PENDING Trade Agreement on the Corda ledger.
 */
@InitiatingFlow
@StartableByRPC
public class ApproveTradeAgreementFlow extends FlowLogic<SignedTransaction> {

    private final String agreementId;

    private final ProgressTracker progressTracker = new ProgressTracker(
            QUERYING_VAULT,
            BUILDING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION
    );

    private static final ProgressTracker.Step QUERYING_VAULT = new ProgressTracker.Step("Querying vault for trade agreement.");
    private static final ProgressTracker.Step BUILDING_TRANSACTION = new ProgressTracker.Step("Building approval transaction.");
    private static final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying approval contract rules.");
    private static final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing approval transaction with buyer private key.");
    private static final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Notarising and updating state in network vaults.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    public ApproveTradeAgreementFlow(String agreementId) {
        this.agreementId = Objects.requireNonNull(agreementId, "agreementId must not be null");
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        progressTracker.setCurrentStep(QUERYING_VAULT);

        // Query the unconsumed states for the trade agreement
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<TradeAgreementState>> states = getServiceHub().getVaultService()
                .queryBy(TradeAgreementState.class, generalCriteria).getStates();

        StateAndRef<TradeAgreementState> inputStateAndRef = null;
        for (StateAndRef<TradeAgreementState> stateAndRef : states) {
            if (stateAndRef.getState().getData().getAgreementId().equalsIgnoreCase(agreementId)) {
                inputStateAndRef = stateAndRef;
                break;
            }
        }

        if (inputStateAndRef == null) {
            throw new FlowException("Trade agreement with ID '" + agreementId + "' was not found in vault or has already been consumed.");
        }

        TradeAgreementState inputAgreement = inputStateAndRef.getState().getData();

        if (inputAgreement.getStatus() != TradeAgreementStatus.PENDING) {
            throw new FlowException("Trade agreement is already in status: " + inputAgreement.getStatus() + ". Only PENDING agreements can be approved.");
        }

        Party buyer = getOurIdentity();
        if (!buyer.equals(inputAgreement.getBuyer())) {
            throw new FlowException("Unauthorized: Only the designated buyer (" +
                    inputAgreement.getBuyer().getName().getOrganisation() +
                    ") can approve this agreement. Current party is: " + buyer.getName().getOrganisation());
        }

        progressTracker.setCurrentStep(BUILDING_TRANSACTION);

        // Notary from input state
        Party notary = inputStateAndRef.getState().getNotary();

        // Build output state in APPROVED status with all business terms preserved
        TradeAgreementState approvedAgreement = new TradeAgreementState(
                inputAgreement.getLinearId(),
                inputAgreement.getAgreementId(),
                inputAgreement.getSeller(),
                inputAgreement.getBuyer(),
                inputAgreement.getProductName(),
                inputAgreement.getDescription(),
                inputAgreement.getQuantity(),
                inputAgreement.getUnit(),
                inputAgreement.getTotalPrice(),
                inputAgreement.getCurrency(),
                inputAgreement.getDeliveryDate(),
                TradeAgreementStatus.APPROVED
        );

        Command<TradeAgreementContract.Commands.Approve> approveCommand = new Command<>(
                new TradeAgreementContract.Commands.Approve(),
                Collections.singletonList(buyer.getOwningKey())
        );

        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(approvedAgreement, TradeAgreementContract.ID)
                .addCommand(approveCommand);

        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        txBuilder.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        FlowSession sellerSession = initiateFlow(inputAgreement.getSeller());

        return subFlow(new FinalityFlow(signedTx, Collections.singletonList(sellerSession), FINALISING_TRANSACTION.childProgressTracker()));
    }
}
