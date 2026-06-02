package com.artelier.api.integration.wompi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WompiMerchantResponse {

    private MerchantData data;

    @Data
    public static class MerchantData {

        @JsonProperty("presigned_acceptance")
        private PresignedAcceptance presignedAcceptance;

        @JsonProperty("presigned_personal_data_auth")
        private PresignedPersonalDataAuth presignedPersonalDataAuth;
    }

    @Data
    public static class PresignedAcceptance {
        @JsonProperty("acceptance_token")
        private String acceptanceToken;
        private String permalink;
        private String type;
    }

    @Data
    public static class PresignedPersonalDataAuth {
        @JsonProperty("acceptance_token")
        private String acceptanceToken;
        private String permalink;
        private String type;
    }
}