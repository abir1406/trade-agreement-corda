package net.corda.tradeagreement.states;

import java.util.Collections;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.tradeagreement.contracts.TradeAgreementContract;
import net.corda.tradeagreement.schema.TradeAgreementSchemaV1;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * TradeAgreementState represents a business trade agreement between a Seller and a Buyer on the Corda ledger.
 */
@BelongsToContract(TradeAgreementContract.class)
public class TradeAgreementState implements LinearState, QueryableState {

    private final UniqueIdentifier linearId;
    private final String agreementId;
    private final Party seller;
    private final Party buyer;
    private final String productName;
    private final String description;
    private final int quantity;
    private final String unit;
    private final BigDecimal totalPrice;
    private final String currency;
    private final Instant deliveryDate;
    private final TradeAgreementStatus status;

    @ConstructorForDeserialization
    public TradeAgreementState(
            UniqueIdentifier linearId,
            String agreementId,
            Party seller,
            Party buyer,
            String productName,
            String description,
            int quantity,
            String unit,
            BigDecimal totalPrice,
            String currency,
            Instant deliveryDate,
            TradeAgreementStatus status) {
        this.linearId = linearId != null ? linearId : new UniqueIdentifier();
        this.agreementId = Objects.requireNonNull(agreementId, "agreementId must not be null");
        this.seller = Objects.requireNonNull(seller, "seller must not be null");
        this.buyer = Objects.requireNonNull(buyer, "buyer must not be null");
        this.productName = Objects.requireNonNull(productName, "productName must not be null");
        this.description = description != null ? description : "";
        this.quantity = quantity;
        this.unit = Objects.requireNonNull(unit, "unit must not be null");
        this.totalPrice = Objects.requireNonNull(totalPrice, "totalPrice must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.deliveryDate = Objects.requireNonNull(deliveryDate, "deliveryDate must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    // Convenience constructor generating linearId
    public TradeAgreementState(
            String agreementId,
            Party seller,
            Party buyer,
            String productName,
            String description,
            int quantity,
            String unit,
            BigDecimal totalPrice,
            String currency,
            Instant deliveryDate,
            TradeAgreementStatus status) {
        this(new UniqueIdentifier(), agreementId, seller, buyer, productName, description, quantity, unit, totalPrice, currency, deliveryDate, status);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public Party getSeller() {
        return seller;
    }

    public Party getBuyer() {
        return buyer;
    }

    public String getProductName() {
        return productName;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getDeliveryDate() {
        return deliveryDate;
    }

    public TradeAgreementStatus getStatus() {
        return status;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(seller, buyer);
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if (schema instanceof TradeAgreementSchemaV1) {
            return new TradeAgreementSchemaV1.PersistentTradeAgreement(
                    this.linearId.getId(),
                    this.agreementId,
                    this.seller.getName().toString(),
                    this.buyer.getName().toString(),
                    this.productName,
                    this.description,
                    this.quantity,
                    this.unit,
                    this.totalPrice,
                    this.currency,
                    this.deliveryDate,
                    this.status.name()
            );
        } else {
            throw new IllegalArgumentException("Unrecognised schema " + schema);
        }
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return Collections.singletonList(new TradeAgreementSchemaV1());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeAgreementState that = (TradeAgreementState) o;
        return quantity == that.quantity &&
                Objects.equals(linearId, that.linearId) &&
                Objects.equals(agreementId, that.agreementId) &&
                Objects.equals(seller, that.seller) &&
                Objects.equals(buyer, that.buyer) &&
                Objects.equals(productName, that.productName) &&
                Objects.equals(description, that.description) &&
                Objects.equals(unit, that.unit) &&
                Objects.equals(totalPrice, that.totalPrice) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(deliveryDate, that.deliveryDate) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linearId, agreementId, seller, buyer, productName, description, quantity, unit, totalPrice, currency, deliveryDate, status);
    }

    @Override
    public String toString() {
        return "TradeAgreementState{" +
                "linearId=" + linearId +
                ", agreementId='" + agreementId + '\'' +
                ", seller=" + seller.getName().getOrganisation() +
                ", buyer=" + buyer.getName().getOrganisation() +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                " " + unit +
                ", totalPrice=" + totalPrice +
                " " + currency +
                ", deliveryDate=" + deliveryDate +
                ", status=" + status +
                '}';
    }
}
