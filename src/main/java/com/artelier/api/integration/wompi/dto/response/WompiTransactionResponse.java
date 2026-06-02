package com.artelier.api.integration.wompi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WompiTransactionResponse {

    private TransactionData data;

    @Data
    public static class TransactionData {

        private String id;
        private String reference;
        private String status;

        @JsonProperty("status_message")
        private String statusMessage;

        @JsonProperty("amount_in_cents")
        private Long amountInCents;

        private String currency;

        @JsonProperty("payment_method_type")
        private String paymentMethodType;

        @JsonProperty("payment_method")
        private PaymentMethodData paymentMethod;
    }

    @Data
    public static class PaymentMethodData {

        private String type;
        private ExtraData extra;
    }

    @Data
    public static class ExtraData {

        @JsonProperty("async_payment_url")
        private String asyncPaymentUrl;

        @JsonProperty("qr_image")
        private String qrImage;

        @JsonProperty("qr_id")
        private String qrId;
    }
}