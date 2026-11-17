package com.artelier.api.integration.mail.Service;

import com.artelier.api.integration.mail.dto.request.SendSmtpEmail;
import com.artelier.api.integration.mail.dto.response.CreateSmtpEmail;
import com.artelier.api.integration.mail.config.BrevoAuthInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Spring Cloud OpenFeign client interface for interacting with the Brevo transactional email API.
 */
@FeignClient(
        name = "brevo-client",
        url = "${brevo.base-url}",
        configuration = BrevoAuthInterceptor.class
)
public interface BrevoService {

    /**
     * Sends a transactional email through the Brevo API endpoint.
     *
     * @param sendSmtpEmail the payload containing email details
     * @return a {@link CreateSmtpEmail} response object containing transaction details
     */
    @PostMapping("/smtp/email")
    CreateSmtpEmail sendTransacEmail(@RequestBody SendSmtpEmail sendSmtpEmail);

}