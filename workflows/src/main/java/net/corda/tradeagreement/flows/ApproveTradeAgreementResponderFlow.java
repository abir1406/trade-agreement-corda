package net.corda.tradeagreement.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.ReceiveFinalityFlow;
import net.corda.core.transactions.SignedTransaction;

/**
 * Responder flow executed on the Seller node to record the approval and updated state in its vault.
 */
@InitiatedBy(ApproveTradeAgreementFlow.class)
public class ApproveTradeAgreementResponderFlow extends net.corda.core.flows.FlowLogic<SignedTransaction> {

    private final FlowSession otherSideSession;

    public ApproveTradeAgreementResponderFlow(FlowSession otherSideSession) {
        this.otherSideSession = otherSideSession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Record the notarized approval transaction and updated ledger state
        return subFlow(new ReceiveFinalityFlow(otherSideSession));
    }
}
