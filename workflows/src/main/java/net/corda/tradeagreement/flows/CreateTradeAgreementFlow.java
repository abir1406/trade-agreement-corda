package net.corda.tradeagreement.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.tradeagreement.contracts.TradeAgreementContract;
import net.corda.tradeagreement.states.TradeAgreementState;
import net.corda.tradeagreement.states.TradeAgreementStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;

/**
 * Initiating flow executed by the Seller to create a new Trade Agreement on the Corda ledger.
 */
@InitiatingFlow
@StartableByRPC
public class CreateTradeAgreementFlow extends FlowLogic<SignedTransaction> {

    private final String agreementId;
    private final Party buyer;
    private final String productName;
    private final String description;
    private final int quantity;
    private final String unit;
    private final BigDecimal totalPrice;
    private final String currency;
    private final Instant deliveryDate;

    private final ProgressTracker progressTracker = new ProgressTracker(
            BUILDING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION
    );

    private static final ProgressTracker.Step BUILDING_TRANSACTION = new ProgressTracker.Step("Building the trade agreement transaction.");
    private static final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
    private static final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
    private static final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Notarising and recording agreement in vault.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    public CreateTradeAgreementFlow(
            String agreementId,
            Party buyer,
            String productName,
            String description,
            int quantity,
            String unit,
            BigDecimal totalPrice,
            String currency,
            Instant deliveryDate) {
        this.agreementId = Objects.requireNonNull(agreementId, "agreementId must not be null");
        this.buyer = Objects.requireNonNull(buyer, "buyer must not be null");
        this.productName = Objects.requireNonNull(productName, "productName must not be null");
        this.description = description != null ? description : "";
        this.quantity = quantity;
        this.unit = Objects.requireNonNull(unit, "unit must not be null");
        this.totalPrice = Objects.requireNonNull(totalPrice, "totalPrice must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.deliveryDate = Objects.requireNonNull(deliveryDate, "deliveryDate must not be null");
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Step 1: Obtain notary identity
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Party seller = getOurIdentity();

        if (seller.equals(buyer)) {
            throw new FlowException("Seller and Buyer must be distinct network identities.");
        }

        progressTracker.setCurrentStep(BUILDING_TRANSACTION);

        // Step 2: Create initial TradeAgreementState with PENDING status
        UniqueIdentifier linearId = new UniqueIdentifier();
        TradeAgreementState outputState = new TradeAgreementState(
                linearId,
                agreementId,
                seller,
                buyer,
                productName,
                description,
                quantity,
                unit,
                totalPrice,
                currency,
                deliveryDate,
                TradeAgreementStatus.PENDING
        );

        // Command with Seller as required signer
        Command<TradeAgreementContract.Commands.Create> createCommand = new Command<>(
                new TradeAgreementContract.Commands.Create(),
                Collections.singletonList(seller.getOwningKey())
        );

        // Step 3: Build transaction
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, TradeAgreementContract.ID)
                .addCommand(createCommand);

        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        txBuilder.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        FlowSession buyerSession = initiateFlow(buyer);

        return subFlow(new FinalityFlow(signedTx, Collections.singletonList(buyerSession), FINALISING_TRANSACTION.childProgressTracker()));
    }
}
