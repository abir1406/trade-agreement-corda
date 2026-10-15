package net.corda.tradeagreement.schema;

import java.util.Collections;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Custom JPA Schema for persisting Trade Agreement data into the Corda Node vault database.
 */
public class TradeAgreementSchemaV1 extends MappedSchema {

    public TradeAgreementSchemaV1() {
        super(TradeAgreementSchema.class, 1, Collections.singletonList(PersistentTradeAgreement.class));
    }

    @Entity
    @Table(name = "trade_agreements")
    public static class PersistentTradeAgreement extends PersistentState {

        @Column(name = "linear_id")
        private UUID linearId;

        @Column(name = "agreement_id", nullable = false)
        private String agreementId;

        @Column(name = "seller_name", nullable = false)
        private String seller;

        @Column(name = "buyer_name", nullable = false)
        private String buyer;

        @Column(name = "product_name", nullable = false)
        private String productName;

        @Column(name = "description", length = 1000)
        private String description;

        @Column(name = "quantity", nullable = false)
        private int quantity;

        @Column(name = "unit", nullable = false)
        private String unit;

        @Column(name = "total_price", precision = 19, scale = 4, nullable = false)
        private BigDecimal totalPrice;

        @Column(name = "currency", length = 10, nullable = false)
        private String currency;

        @Column(name = "delivery_date", nullable = false)
        private Instant deliveryDate;

        @Column(name = "status", length = 20, nullable = false)
        private String status;

        public PersistentTradeAgreement() {
        }

        public PersistentTradeAgreement(
                UUID linearId,
                String agreementId,
                String seller,
                String buyer,
                String productName,
                String description,
                int quantity,
                String unit,
                BigDecimal totalPrice,
                String currency,
                Instant deliveryDate,
                String status) {
            this.linearId = linearId;
            this.agreementId = agreementId;
            this.seller = seller;
            this.buyer = buyer;
            this.productName = productName;
            this.description = description;
            this.quantity = quantity;
            this.unit = unit;
            this.totalPrice = totalPrice;
            this.currency = currency;
            this.deliveryDate = deliveryDate;
            this.status = status;
        }

        public UUID getLinearId() {
            return linearId;
        }

        public String getAgreementId() {
            return agreementId;
        }

        public String getSeller() {
            return seller;
        }

        public String getBuyer() {
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

        public String getStatus() {
            return status;
        }
    }
}
