package com.artelier.api.integration.wompi.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class WompiFinancialInstitutionsListResponse {
    private List<WompiFinancialInstitutionsResponse> data;
}

