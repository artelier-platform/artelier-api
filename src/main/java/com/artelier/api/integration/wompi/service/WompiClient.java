package com.artelier.api.integration.wompi.service;

import com.artelier.api.integration.wompi.config.WompiAuthRequestInterceptor;
import com.artelier.api.integration.wompi.dto.request.WompiTransactionRequest;
import com.artelier.api.integration.wompi.dto.response.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "wompi",
        url = "${wompi.base-url}",
        configuration = WompiAuthRequestInterceptor.class
)
public interface WompiClient {

    @GetMapping("/merchants/{key}")
    WompiMerchantResponse getMerchant(@PathVariable("key") String key);

    @PostMapping("/transactions")
    WompiTransactionResponse createTransaction(@RequestBody WompiTransactionRequest request);

    @GetMapping("/transactions/{transactionId}")
    WompiTransactionResponse getTransaction(@PathVariable("transactionId") String transactionId);

    @GetMapping("/pse/financial_institutions")
    WompiFinancialInstitutionsListResponse getFinancialInstitutions();
}