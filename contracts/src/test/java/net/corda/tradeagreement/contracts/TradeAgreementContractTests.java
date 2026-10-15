package net.corda.tradeagreement.contracts;

import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import net.corda.tradeagreement.states.TradeAgreementState;
import net.corda.tradeagreement.states.TradeAgreementStatus;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class TradeAgreementContractTests {

    private final MockServices ledgerServices = new MockServices(
            Collections.singletonList("net.corda.tradeagreement.contracts")
    );

    private final TestIdentity sellerId = new TestIdentity(new CordaX500Name("ABCElectronics", "New York", "US"));
    private final TestIdentity buyerId = new TestIdentity(new CordaX500Name("XYZRetailers", "Paris", "FR"));

    private final Party seller = sellerId.getParty();
    private final Party buyer = buyerId.getParty();

    private final Instant futureDate = Instant.now().plus(30, ChronoUnit.DAYS);

    private TradeAgreementState createSampleAgreement(TradeAgreementStatus status, int quantity, BigDecimal price) {
        return new TradeAgreementState(
                "AGR-2026-001",
                seller,
                buyer,
                "Laptops",
                "High-performance enterprise laptops",
                quantity,
                "units",
                price,
                "INR",
                futureDate,
                status
        );
    }

    @Test
    public void validTradeAgreementCreationPasses() {
        TradeAgreementState agreement = createSampleAgreement(TradeAgreementStatus.PENDING, 100, new BigDecimal("1000000.00"));
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(TradeAgreementContract.ID, agreement);
                tx.command(seller.getOwningKey(), new TradeAgreementContract.Commands.Create());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void creationFailsIfInputsPresent() {
        TradeAgreementState agreement = createSampleAgreement(TradeAgreementStatus.PENDING, 100, new BigDecimal("1000000.00"));
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(TradeAgreementContract.ID, agreement);
                tx.output(TradeAgreementContract.ID, agreement);
                tx.command(seller.getOwningKey(), new TradeAgreementContract.Commands.Create());
                tx.failsWith("No inputs should be consumed when creating a trade agreement.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void creationFailsIfQuantityNotPositive() {
        TradeAgreementState agreement = createSampleAgreement(TradeAgreementStatus.PENDING, 0, new BigDecimal("1000000.00"));
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(TradeAgreementContract.ID, agreement);
                tx.command(seller.getOwningKey(), new TradeAgreementContract.Commands.Create());
                tx.failsWith("Quantity must be strictly positive.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void creationFailsIfPriceNotPositive() {
        TradeAgreementState agreement = createSampleAgreement(TradeAgreementStatus.PENDING, 100, BigDecimal.ZERO);
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(TradeAgreementContract.ID, agreement);
                tx.command(seller.getOwningKey(), new TradeAgreementContract.Commands.Create());
                tx.failsWith("Total price must be strictly positive.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void creationFailsIfSellerEqualsBuyer() {
        TradeAgreementState agreement = new TradeAgreementState(
                "AGR-2026-001",
                seller,
                seller, // same party!
                "Laptops",
                "High-performance enterprise laptops",
                100,
                "units",
                new BigDecimal("1000000.00"),
                "INR",
                futureDate,
                TradeAgreementStatus.PENDING
        );
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(TradeAgreementContract.ID, agreement);
                tx.command(seller.getOwningKey(), new TradeAgreementContract.Commands.Create());
                tx.failsWith("Seller and Buyer must be distinct parties.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void creationFailsIfInitialStatusNotPending() {
        TradeAgreementState agreement = createSampleAgreement(TradeAgreementStatus.APPROVED, 100, new BigDecimal("1000000.00"));
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(TradeAgreementContract.ID, agreement);
                tx.command(seller.getOwningKey(), new TradeAgreementContract.Commands.Create());
                tx.failsWith("Initial agreement status must be PENDING.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void creationFailsIfSellerSignatureMissing() {
        TradeAgreementState agreement = createSampleAgreement(TradeAgreementStatus.PENDING, 100, new BigDecimal("1000000.00"));
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(TradeAgreementContract.ID, agreement);
                tx.command(buyer.getOwningKey(), new TradeAgreementContract.Commands.Create()); // signed by buyer instead of seller
                tx.failsWith("The seller must sign the creation of the trade agreement.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void validApprovalPasses() {
        TradeAgreementState pending = createSampleAgreement(TradeAgreementStatus.PENDING, 100, new BigDecimal("1000000.00"));
        TradeAgreementState approved = new TradeAgreementState(
                pending.getLinearId(),
                pending.getAgreementId(),
                pending.getSeller(),
                pending.getBuyer(),
                pending.getProductName(),
                pending.getDescription(),
                pending.getQuantity(),
                pending.getUnit(),
                pending.getTotalPrice(),
                pending.getCurrency(),
                pending.getDeliveryDate(),
                TradeAgreementStatus.APPROVED
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(TradeAgreementContract.ID, pending);
                tx.output(TradeAgreementContract.ID, approved);
                tx.command(buyer.getOwningKey(), new TradeAgreementContract.Commands.Approve());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void approvalFailsIfBuyerSignatureMissing() {
        TradeAgreementState pending = createSampleAgreement(TradeAgreementStatus.PENDING, 100, new BigDecimal("1000000.00"));
        TradeAgreementState approved = new TradeAgreementState(
                pending.getLinearId(),
                pending.getAgreementId(),
                pending.getSeller(),
                pending.getBuyer(),
                pending.getProductName(),
                pending.getDescription(),
                pending.getQuantity(),
                pending.getUnit(),
                pending.getTotalPrice(),
                pending.getCurrency(),
                pending.getDeliveryDate(),
                TradeAgreementStatus.APPROVED
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(TradeAgreementContract.ID, pending);
                tx.output(TradeAgreementContract.ID, approved);
                tx.command(seller.getOwningKey(), new TradeAgreementContract.Commands.Approve()); // signed by seller instead of buyer
                tx.failsWith("The buyer must sign the trade agreement approval.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void approvalFailsIfPriceTampered() {
        TradeAgreementState pending = createSampleAgreement(TradeAgreementStatus.PENDING, 100, new BigDecimal("1000000.00"));
        TradeAgreementState approvedWithModifiedPrice = new TradeAgreementState(
                pending.getLinearId(),
                pending.getAgreementId(),
                pending.getSeller(),
                pending.getBuyer(),
                pending.getProductName(),
                pending.getDescription(),
                pending.getQuantity(),
                pending.getUnit(),
                new BigDecimal("800000.00"), // tampered price!
                pending.getCurrency(),
                pending.getDeliveryDate(),
                TradeAgreementStatus.APPROVED
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(TradeAgreementContract.ID, pending);
                tx.output(TradeAgreementContract.ID, approvedWithModifiedPrice);
                tx.command(buyer.getOwningKey(), new TradeAgreementContract.Commands.Approve());
                tx.failsWith("Total price must not be modified.");
                return null;
            });
            return null;
        });
    }
}
