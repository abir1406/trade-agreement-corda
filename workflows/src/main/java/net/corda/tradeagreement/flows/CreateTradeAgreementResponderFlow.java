package net.corda.tradeagreement.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

/**
 * Responder flow executed on the Buyer node to receive and record the created Trade Agreement in its vault.
 */
@InitiatedBy(CreateTradeAgreementFlow.class)
public class CreateTradeAgreementResponderFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession otherSideSession;

    public CreateTradeAgreementResponderFlow(FlowSession otherSideSession) {
        this.otherSideSession = otherSideSession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Automatically verify and record the notarized transaction into vault
        return subFlow(new ReceiveFinalityFlow(otherSideSession));
    }
}
