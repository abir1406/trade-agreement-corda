package net.corda.tradeagreement.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.tradeagreement.states.TradeAgreementState;
import net.corda.tradeagreement.states.TradeAgreementStatus;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * TradeAgreementContract governs the creation and approval rules for Trade Agreements on the Corda ledger.
 */
public class TradeAgreementContract implements Contract {

    public static final String ID = "net.corda.tradeagreement.contracts.TradeAgreementContract";

    public interface Commands extends CommandData {
        class Create implements Commands {}
        class Approve implements Commands {}
    }

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        if (commandData instanceof Commands.Create) {
            verifyCreate(tx, command.getSigners());
        } else if (commandData instanceof Commands.Approve) {
            verifyApprove(tx, command.getSigners());
        } else {
            throw new IllegalArgumentException("Unrecognised command: " + commandData);
        }
    }

    private void verifyCreate(LedgerTransaction tx, List<PublicKey> signers) {
        requireThat(require -> {
            // Shape rules
            require.using("No inputs should be consumed when creating a trade agreement.",
                    tx.getInputs().isEmpty());
            require.using("Only one output state should be created when creating a trade agreement.",
                    tx.getOutputs().size() == 1);

            ContractState outputContractState = tx.getOutputs().get(0).getData();
            require.using("Output state must be a TradeAgreementState.",
                    outputContractState instanceof TradeAgreementState);

            TradeAgreementState out = (TradeAgreementState) outputContractState;

            // Business logic rules
            require.using("Agreement ID must not be blank.",
                    out.getAgreementId() != null && !out.getAgreementId().trim().isEmpty());

            require.using("Product name must not be blank.",
                    out.getProductName() != null && !out.getProductName().trim().isEmpty());

            require.using("Unit must not be blank.",
                    out.getUnit() != null && !out.getUnit().trim().isEmpty());

            require.using("Currency must not be blank.",
                    out.getCurrency() != null && !out.getCurrency().trim().isEmpty());

            require.using("Seller and Buyer must be distinct parties.",
                    !out.getSeller().equals(out.getBuyer()));

            require.using("Quantity must be strictly positive.",
                    out.getQuantity() > 0);

            require.using("Total price must be strictly positive.",
                    out.getTotalPrice() != null && out.getTotalPrice().compareTo(BigDecimal.ZERO) > 0);

            require.using("Delivery date must not be null.",
                    out.getDeliveryDate() != null);

            require.using("Initial agreement status must be PENDING.",
                    out.getStatus() == TradeAgreementStatus.PENDING);

            // Signer rules: The seller must sign the creation transaction
            require.using("The seller must sign the creation of the trade agreement.",
                    signers.contains(out.getSeller().getOwningKey()));

            return null;
        });
    }

    private void verifyApprove(LedgerTransaction tx, List<PublicKey> signers) {
        requireThat(require -> {
            // Shape rules
            require.using("Exactly one input state must be consumed when approving a trade agreement.",
                    tx.getInputs().size() == 1);
            require.using("Exactly one output state must be produced when approving a trade agreement.",
                    tx.getOutputs().size() == 1);

            ContractState inputContractState = tx.getInputs().get(0).getState().getData();
            ContractState outputContractState = tx.getOutputs().get(0).getData();

            require.using("Input state must be a TradeAgreementState.",
                    inputContractState instanceof TradeAgreementState);
            require.using("Output state must be a TradeAgreementState.",
                    outputContractState instanceof TradeAgreementState);

            TradeAgreementState in = (TradeAgreementState) inputContractState;
            TradeAgreementState out = (TradeAgreementState) outputContractState;

            // Business rules
            require.using("Only trade agreements in PENDING status can be approved.",
                    in.getStatus() == TradeAgreementStatus.PENDING);

            require.using("The output trade agreement status must be APPROVED.",
                    out.getStatus() == TradeAgreementStatus.APPROVED);

            require.using("Agreement ID must not be modified.",
                    in.getAgreementId().equals(out.getAgreementId()));

            require.using("Linear ID must not be modified.",
                    in.getLinearId().equals(out.getLinearId()));

            require.using("Seller must not be modified.",
                    in.getSeller().equals(out.getSeller()));

            require.using("Buyer must not be modified.",
                    in.getBuyer().equals(out.getBuyer()));

            require.using("Product name must not be modified.",
                    in.getProductName().equals(out.getProductName()));

            require.using("Quantity must not be modified.",
                    in.getQuantity() == out.getQuantity());

            require.using("Unit must not be modified.",
                    in.getUnit().equals(out.getUnit()));

            require.using("Total price must not be modified.",
                    in.getTotalPrice().compareTo(out.getTotalPrice()) == 0);

            require.using("Currency must not be modified.",
                    in.getCurrency().equals(out.getCurrency()));

            require.using("Delivery date must not be modified.",
                    in.getDeliveryDate().equals(out.getDeliveryDate()));

            // Signer rules: The buyer must sign the approval transaction
            require.using("The buyer must sign the trade agreement approval.",
                    signers.contains(in.getBuyer().getOwningKey()));

            return null;
        });
    }
}
