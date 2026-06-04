package com.artelier.api.integration.wompi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WompiFinancialInstitutionsResponse {

    @JsonProperty("financial_institution_code")
    private String code;

    @JsonProperty("financial_institution_name")
    private String name;
}