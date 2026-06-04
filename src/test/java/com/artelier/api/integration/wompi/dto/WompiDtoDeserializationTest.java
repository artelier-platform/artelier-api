package com.artelier.api.integration.wompi.dto;

import com.artelier.api.integration.wompi.dto.response.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WompiDtoDeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ─── WompiMerchantResponse ────────────────────────────────────────────────

    @Test
    void shouldDeserializeMerchantResponse() throws Exception {
        String json = """
                {
                  "data": {
                    "presigned_acceptance": {
                      "acceptance_token": "tok_acceptance_abc",
                      "permalink": "https://wompi.com/terms",
                      "type": "END_USER_POLICY"
                    },
                    "presigned_personal_data_auth": {
                      "acceptance_token": "tok_personal_xyz",
                      "permalink": "https://wompi.com/privacy",
                      "type": "PERSONAL_DATA_AUTH"
                    }
                  }
                }
                """;

        WompiMerchantResponse result = objectMapper.readValue(json, WompiMerchantResponse.class);

        assertNotNull(result.getData());
        assertEquals("tok_acceptance_abc", result.getData().getPresignedAcceptance().getAcceptanceToken());
        assertEquals("https://wompi.com/terms", result.getData().getPresignedAcceptance().getPermalink());
        assertEquals("END_USER_POLICY", result.getData().getPresignedAcceptance().getType());
        assertEquals("tok_personal_xyz", result.getData().getPresignedPersonalDataAuth().getAcceptanceToken());
        assertEquals("https://wompi.com/privacy", result.getData().getPresignedPersonalDataAuth().getPermalink());
        assertEquals("PERSONAL_DATA_AUTH", result.getData().getPresignedPersonalDataAuth().getType());
    }

    @Test
    void shouldDeserializeMerchantResponseWithNullOptionalFields() throws Exception {
        String json = """
                {
                  "data": {
                    "presigned_acceptance": {
                      "acceptance_token": "tok_acceptance_abc"
                    },
                    "presigned_personal_data_auth": {
                      "acceptance_token": "tok_personal_xyz"
                    }
                  }
                }
                """;

        WompiMerchantResponse result = objectMapper.readValue(json, WompiMerchantResponse.class);

        assertEquals("tok_acceptance_abc", result.getData().getPresignedAcceptance().getAcceptanceToken());
        assertNull(result.getData().getPresignedAcceptance().getPermalink());
        assertNull(result.getData().getPresignedAcceptance().getType());
    }

    // ─── WompiTransactionResponse ─────────────────────────────────────────────

    @Test
    void shouldDeserializeApprovedCardTransactionResponse() throws Exception {
        String json = """
                {
                  "data": {
                    "id": "wompi_tx_001",
                    "reference": "ORDER-abc-123",
                    "status": "APPROVED",
                    "status_message": null,
                    "amount_in_cents": 100000,
                    "currency": "COP",
                    "payment_method_type": "CARD",
                    "payment_method": {
                      "type": "CARD",
                      "extra": {}
                    }
                  }
                }
                """;

        WompiTransactionResponse result = objectMapper.readValue(json, WompiTransactionResponse.class);

        assertNotNull(result.getData());
        assertEquals("wompi_tx_001",   result.getData().getId());
        assertEquals("ORDER-abc-123",  result.getData().getReference());
        assertEquals("APPROVED",       result.getData().getStatus());
        assertNull(result.getData().getStatusMessage());
        assertEquals(100000L,          result.getData().getAmountInCents());
        assertEquals("COP",            result.getData().getCurrency());
        assertEquals("CARD",           result.getData().getPaymentMethodType());
        assertEquals("CARD",           result.getData().getPaymentMethod().getType());
    }

    @Test
    void shouldDeserializePseTransactionResponseWithAsyncPaymentUrl() throws Exception {
        String json = """
                {
                  "data": {
                    "id": "wompi_tx_002",
                    "reference": "ORDER-pse-456",
                    "status": "PENDING",
                    "status_message": "Esperando pago PSE",
                    "amount_in_cents": 500000,
                    "currency": "COP",
                    "payment_method_type": "PSE",
                    "payment_method": {
                      "type": "PSE",
                      "extra": {
                        "async_payment_url": "https://pse.com/pay/abc123"
                      }
                    }
                  }
                }
                """;

        WompiTransactionResponse result = objectMapper.readValue(json, WompiTransactionResponse.class);

        assertEquals("PENDING",                       result.getData().getStatus());
        assertEquals("Esperando pago PSE",            result.getData().getStatusMessage());
        assertEquals("PSE",                           result.getData().getPaymentMethodType());
        assertEquals("https://pse.com/pay/abc123",
                result.getData().getPaymentMethod().getExtra().getAsyncPaymentUrl());
        assertNull(result.getData().getPaymentMethod().getExtra().getQrImage());
        assertNull(result.getData().getPaymentMethod().getExtra().getQrId());
    }

    @Test
    void shouldDeserializeNequiTransactionResponseWithQrFields() throws Exception {
        String json = """
                {
                  "data": {
                    "id": "wompi_tx_003",
                    "reference": "ORDER-nequi-789",
                    "status": "PENDING",
                    "status_message": null,
                    "amount_in_cents": 200000,
                    "currency": "COP",
                    "payment_method_type": "NEQUI",
                    "payment_method": {
                      "type": "NEQUI",
                      "extra": {
                        "qr_image": "data:image/png;base64,abc==",
                        "qr_id": "qr_nequi_001"
                      }
                    }
                  }
                }
                """;

        WompiTransactionResponse result = objectMapper.readValue(json, WompiTransactionResponse.class);

        assertEquals("NEQUI",                        result.getData().getPaymentMethodType());
        assertEquals("data:image/png;base64,abc==",
                result.getData().getPaymentMethod().getExtra().getQrImage());
        assertEquals("qr_nequi_001",
                result.getData().getPaymentMethod().getExtra().getQrId());
        assertNull(result.getData().getPaymentMethod().getExtra().getAsyncPaymentUrl());
    }

    @Test
    void shouldDeserializeDeclinedTransactionWithStatusMessage() throws Exception {
        String json = """
                {
                  "data": {
                    "id": "wompi_tx_004",
                    "reference": "ORDER-declined-000",
                    "status": "DECLINED",
                    "status_message": "Fondos insuficientes",
                    "amount_in_cents": 100000,
                    "currency": "COP",
                    "payment_method_type": "CARD",
                    "payment_method": {
                      "type": "CARD",
                      "extra": {}
                    }
                  }
                }
                """;

        WompiTransactionResponse result = objectMapper.readValue(json, WompiTransactionResponse.class);

        assertEquals("DECLINED",              result.getData().getStatus());
        assertEquals("Fondos insuficientes",  result.getData().getStatusMessage());
    }

    // ─── WompiFinancialInstitutionsListResponse ───────────────────────────────

    @Test
    void shouldDeserializeFinancialInstitutionsList() throws Exception {
        String json = """
                {
                  "data": [
                    {
                      "financial_institution_code": "1007",
                      "financial_institution_name": "Bancolombia"
                    },
                    {
                      "financial_institution_code": "1022",
                      "financial_institution_name": "Banco de Bogotá"
                    }
                  ]
                }
                """;

        WompiFinancialInstitutionsListResponse result =
                objectMapper.readValue(json, WompiFinancialInstitutionsListResponse.class);

        List<WompiFinancialInstitutionsResponse> institutions = result.getData();

        assertNotNull(institutions);
        assertEquals(2, institutions.size());

        assertEquals("1007",          institutions.get(0).getCode());
        assertEquals("Bancolombia",   institutions.get(0).getName());
        assertEquals("1022",          institutions.get(1).getCode());
        assertEquals("Banco de Bogotá", institutions.get(1).getName());
    }

    @Test
    void shouldDeserializeEmptyFinancialInstitutionsList() throws Exception {
        String json = """
                {
                  "data": []
                }
                """;

        WompiFinancialInstitutionsListResponse result =
                objectMapper.readValue(json, WompiFinancialInstitutionsListResponse.class);

        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }
}