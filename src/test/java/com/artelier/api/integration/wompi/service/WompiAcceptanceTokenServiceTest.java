package com.artelier.api.integration.wompi.service;

import com.artelier.api.integration.wompi.dto.request.WompiAcceptanceTokens;
import com.artelier.api.integration.wompi.dto.response.WompiMerchantResponse;
import com.artelier.api.integration.wompi.service.impl.WompiAcceptanceTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WompiAcceptanceTokenServiceTest {

    @Mock
    private WompiClient wompiClient;

    @InjectMocks
    private WompiAcceptanceTokenServiceImpl service;

    private static final String TEST_PUBLIC_KEY      = "pub_test_abc123";
    private static final String ACCEPTANCE_TOKEN     = "acceptance_tok_xyz";
    private static final String PERSONAL_AUTH_TOKEN  = "personal_auth_tok_xyz";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "publicKey", TEST_PUBLIC_KEY);
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Test
    void shouldReturnTokensFromMerchantResponse() {
        when(wompiClient.getMerchant(TEST_PUBLIC_KEY)).thenReturn(buildMerchantResponse(
                ACCEPTANCE_TOKEN, PERSONAL_AUTH_TOKEN
        ));

        WompiAcceptanceTokens result = service.getTokens();

        assertNotNull(result);
        assertEquals(ACCEPTANCE_TOKEN, result.getAcceptanceToken());
        assertEquals(PERSONAL_AUTH_TOKEN, result.getAcceptPersonalAuth());
    }

    @Test
    void shouldCallWompiClientWithPublicKey() {
        when(wompiClient.getMerchant(TEST_PUBLIC_KEY)).thenReturn(buildMerchantResponse(
                ACCEPTANCE_TOKEN, PERSONAL_AUTH_TOKEN
        ));

        service.getTokens();

        // Verifica que se llama con la public key correcta, no con otra
        verify(wompiClient, times(1)).getMerchant(TEST_PUBLIC_KEY);
        verify(wompiClient, never()).getMerchant(argThat(k -> !k.equals(TEST_PUBLIC_KEY)));
    }

    @Test
    void shouldReturnDifferentTokensWhenMerchantResponseVaries() {
        String otherAcceptance    = "other_acceptance_tok";
        String otherPersonalAuth  = "other_personal_auth_tok";

        when(wompiClient.getMerchant(TEST_PUBLIC_KEY)).thenReturn(buildMerchantResponse(
                otherAcceptance, otherPersonalAuth
        ));

        WompiAcceptanceTokens result = service.getTokens();

        assertEquals(otherAcceptance,   result.getAcceptanceToken());
        assertEquals(otherPersonalAuth, result.getAcceptPersonalAuth());
    }

    // ─── Propagación de errores del cliente ──────────────────────────────────

    @Test
    void shouldPropagateExceptionWhenWompiClientFails() {
        when(wompiClient.getMerchant(TEST_PUBLIC_KEY))
                .thenThrow(new RuntimeException("Wompi no disponible"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getTokens());

        assertEquals("Wompi no disponible", ex.getMessage());
    }

    @Test
    void shouldThrowWhenMerchantDataIsNull() {
        WompiMerchantResponse response = new WompiMerchantResponse();
        response.setData(null);

        when(wompiClient.getMerchant(TEST_PUBLIC_KEY)).thenReturn(response);

        // getData() retorna null → .getPresignedAcceptance() lanza NPE
        assertThrows(NullPointerException.class, () -> service.getTokens());
    }

    @Test
    void shouldThrowWhenPresignedAcceptanceIsNull() {
        WompiMerchantResponse.MerchantData data = new WompiMerchantResponse.MerchantData();
        data.setPresignedAcceptance(null);
        data.setPresignedPersonalDataAuth(buildPersonalDataAuth(PERSONAL_AUTH_TOKEN));

        WompiMerchantResponse response = new WompiMerchantResponse();
        response.setData(data);

        when(wompiClient.getMerchant(TEST_PUBLIC_KEY)).thenReturn(response);

        assertThrows(NullPointerException.class, () -> service.getTokens());
    }

    @Test
    void shouldThrowWhenPresignedPersonalDataAuthIsNull() {
        WompiMerchantResponse.MerchantData data = new WompiMerchantResponse.MerchantData();
        data.setPresignedAcceptance(buildPresignedAcceptance(ACCEPTANCE_TOKEN));
        data.setPresignedPersonalDataAuth(null);

        WompiMerchantResponse response = new WompiMerchantResponse();
        response.setData(data);

        when(wompiClient.getMerchant(TEST_PUBLIC_KEY)).thenReturn(response);

        assertThrows(NullPointerException.class, () -> service.getTokens());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private WompiMerchantResponse buildMerchantResponse(String acceptanceToken, String personalAuthToken) {
        WompiMerchantResponse.MerchantData data = new WompiMerchantResponse.MerchantData();
        data.setPresignedAcceptance(buildPresignedAcceptance(acceptanceToken));
        data.setPresignedPersonalDataAuth(buildPersonalDataAuth(personalAuthToken));

        WompiMerchantResponse response = new WompiMerchantResponse();
        response.setData(data);
        return response;
    }

    private WompiMerchantResponse.PresignedAcceptance buildPresignedAcceptance(String token) {
        WompiMerchantResponse.PresignedAcceptance acceptance = new WompiMerchantResponse.PresignedAcceptance();
        acceptance.setAcceptanceToken(token);
        return acceptance;
    }

    private WompiMerchantResponse.PresignedPersonalDataAuth buildPersonalDataAuth(String token) {
        WompiMerchantResponse.PresignedPersonalDataAuth auth = new WompiMerchantResponse.PresignedPersonalDataAuth();
        auth.setAcceptanceToken(token);
        return auth;
    }
}