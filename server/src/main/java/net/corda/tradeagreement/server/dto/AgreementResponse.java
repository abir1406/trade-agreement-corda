package net.corda.tradeagreement.server.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class AgreementResponse {

    private String agreementId;
    private String linearId;
    private String seller;
    private String buyer;
    private String productName;
    private String description;
    private int quantity;
    private String unit;
    private BigDecimal totalPrice;
    private String currency;
    private Instant deliveryDate;
    private String status;
    private String txId;
    private String notary;
    private boolean consumed;

    public AgreementResponse() {
    }

    public AgreementResponse(
            String agreementId,
            String linearId,
            String seller,
            String buyer,
            String productName,
            String description,
            int quantity,
            String unit,
            BigDecimal totalPrice,
            String currency,
            Instant deliveryDate,
            String status,
            String txId,
            String notary,
            boolean consumed) {
        this.agreementId = agreementId;
        this.linearId = linearId;
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
        this.txId = txId;
        this.notary = notary;
        this.consumed = consumed;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public String getLinearId() {
        return linearId;
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

    public String getTxId() {
        return txId;
    }

    public String getNotary() {
        return notary;
    }

    public boolean isConsumed() {
        return consumed;
    }
}
