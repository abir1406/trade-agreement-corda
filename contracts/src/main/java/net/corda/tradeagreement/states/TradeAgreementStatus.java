package net.corda.tradeagreement.states;

import net.corda.core.serialization.CordaSerializable;

/**
 * Represents the lifecycle status of a Trade Agreement on the Corda ledger.
 */
@CordaSerializable
public enum TradeAgreementStatus {
    PENDING,
    APPROVED,
    REJECTED
}
