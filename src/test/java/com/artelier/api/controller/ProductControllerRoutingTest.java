package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.config.SecurityConfig;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Routing edge cases of the public catalog: {@code /products/**} is {@code permitAll} for every
 * HTTP method, so requests with no handler reach Spring MVC. These tests document the expected
 * contract (405 for an unsupported method, 404 for an unknown path) and are meant to confirm
 * whether {@code GlobalExceptionHandler}'s catch-all {@code Exception} handler turns them into 500.
 */
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JacksonTestConfig.class})
@DisplayName("ProductController – unmapped routes")
class ProductControllerRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @ParameterizedTest(name = "{0} {1} -> 405")
    @CsvSource({
            "POST,   /products",
            "PUT,    /products",
            "DELETE, /products",
            "PATCH,  /products",
            "POST,   /products/handmade-ceramic-mug",
            "PUT,    /products/handmade-ceramic-mug",
            "DELETE, /products/handmade-ceramic-mug",
            "PATCH,  /products/handmade-ceramic-mug"
    })
    @DisplayName("405 – unsupported method on a public product route")
    void unsupportedMethod(String method, String uri) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), uri))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "GET {0} -> 404")
    @CsvSource({
            "/products/a/b",
            "/products/a/b/c"
    })
    @DisplayName("404 – unknown product path")
    void unknownPath(String uri) throws Exception {
        mockMvc.perform(get(uri))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(productService);
    }
}
