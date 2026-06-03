package com.artelier.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(
        name = "PaymentWebhookRequest",
        description = "Webhook payload recibido desde Wompi cuando cambia el estado de una transacción"
)
public class PaymentWebhookRequest {

    @Schema(description = "Tipo de evento de Wompi", example = "transaction.updated")
    private String event;

    @Schema(description = "Wrapper del payload de la transacción")
    private TransactionData data;

    @Schema(description = "Datos de firma para validar autenticidad del webhook")
    private SignatureData signature;

    @Schema(description = "Unix timestamp del evento en milisegundos", example = "1715000000000")
    private Long timestamp;

    @Data
    @Schema(description = "Wrapper de la transacción")
    public static class TransactionData {

        @Schema(description = "Detalle de la transacción")
        private Transaction transaction;
    }

    @Data
    @Schema(description = "Datos de la transacción de Wompi")
    public static class Transaction {

        @Schema(description = "ID único de la transacción en Wompi", example = "1292-1602113476-10985")
        private String id;

        @Schema(description = "Referencia del comercio", example = "ORDER-550e8400-1715000000000")
        private String reference;

        @Schema(
                description = "Estado de la transacción",
                example = "APPROVED",
                allowableValues = {"PENDING", "APPROVED", "DECLINED", "VOIDED", "ERROR"}
        )
        private String status;

        @Schema(
                description = "Método de pago usado por el cliente",
                example = "NEQUI",
                allowableValues = {"CARD", "NEQUI", "PSE", "BANCOLOMBIA_TRANSFER", "BANCOLOMBIA_QR"}
        )
        @JsonProperty("payment_method_type")
        private String paymentMethodType;

        @Schema(description = "Monto en centavos", example = "100000")
        @JsonProperty("amount_in_cents")
        private Long amountInCents;
    }

    @Data
    @Schema(description = "Bloque de firma HMAC para verificar autenticidad del webhook")
    public static class SignatureData {

        @Schema(description = "Checksum SHA-256 generado por Wompi", example = "a3f5c9d8e1b2...")
        private String checksum;

        @Schema(
                description = "Lista de propiedades usadas para generar la firma",
                example = "[\"transaction.id\", \"transaction.status\", \"transaction.amount_in_cents\"]"
        )
        private List<String> properties;
    }
}