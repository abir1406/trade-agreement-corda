package net.corda.tradeagreement.server.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CreateAgreementRequest {

    public static class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {
        private static final DateTimeFormatter[] FORMATTERS = new DateTimeFormatter[]{
                DateTimeFormatter.ISO_INSTANT,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC),
                DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC),
                DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneOffset.UTC),
                DateTimeFormatter.ofPattern("MM/dd/yyyy").withZone(ZoneOffset.UTC)
        };

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            text = text.trim();
            for (DateTimeFormatter f : FORMATTERS) {
                try {
                    return Instant.from(f.parse(text));
                } catch (Exception ignored) {
                }
            }
            try {
                return LocalDate.parse(text, DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (Exception ignored) {
            }
            try {
                return LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (Exception ignored) {
            }
            try {
                return Instant.ofEpochMilli(Long.parseLong(text));
            } catch (Exception ignored) {
            }
            throw new IllegalArgumentException("Unable to parse delivery date: '" + text + "'. Expected format: YYYY-MM-DD or DD-MM-YYYY.");
        }
    }

    private String agreementId;

    @JsonAlias({"buyer", "buyerParty"})
    @NotBlank(message = "Buyer party name is required")
    private String buyerParty;

    @NotBlank(message = "Product name is required")
    private String productName;

    private String description;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @NotBlank(message = "Unit of measurement is required")
    private String unit = "units";

    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.01", message = "Total price must be greater than zero")
    private BigDecimal totalPrice;

    @NotBlank(message = "Currency is required")
    private String currency = "INR";

    @NotNull(message = "Delivery date is required")
    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant deliveryDate;

    public CreateAgreementRequest() {
    }

    public String getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(String agreementId) {
        this.agreementId = agreementId;
    }

    public String getBuyerParty() {
        return buyerParty;
    }

    public void setBuyerParty(String buyerParty) {
        this.buyerParty = buyerParty;
    }

    public void setBuyer(String buyer) {
        if (this.buyerParty == null || this.buyerParty.trim().isEmpty()) {
            this.buyerParty = buyer;
        }
    }

    public String getBuyer() {
        return this.buyerParty;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        if (unit == null || unit.trim().isEmpty()) {
            this.unit = "units";
        } else {
            this.unit = unit;
        }
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Instant deliveryDate) {
        this.deliveryDate = deliveryDate;
    }
}
