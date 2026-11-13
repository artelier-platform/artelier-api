package com.artelier.api.auth.dto;

import com.artelier.api.auth.dto.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import com.artelier.api.user.entity.User;
import com.artelier.api.verification.dto.PhoneVerificationRequest;

@DisplayName("RegisterRequest validation")
class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private static RegisterRequest build(String username, String password, String firstName,
                                         String lastName, String email,
                                         String countryCode, String phone) {
        return new RegisterRequest(username, password, firstName, lastName,
                email, countryCode, phone);
    }

    private static RegisterRequest valid() {
        return build("Geronimo01", "StrongPass123", "Geronimo", "Rodriguez",
                "user@example.com", "+57", "3001234567");
    }

    private static RegisterRequest withUsername(String v) {
        return build(v, "StrongPass123", "Geronimo", "Rodriguez", "user@example.com", "+57", "3001234567");
    }

    private static RegisterRequest withPassword(String v) {
        return build("Geronimo01", v, "Geronimo", "Rodriguez", "user@example.com", "+57", "3001234567");
    }

    private static RegisterRequest withNames(String first, String last) {
        return build("Geronimo01", "StrongPass123", first, last, "user@example.com", "+57", "3001234567");
    }

    private static RegisterRequest withCountryCode(String v) {
        return build("Geronimo01", "StrongPass123", "Geronimo", "Rodriguez", "user@example.com", v, "3001234567");
    }

    private static RegisterRequest withPhone(String v) {
        return build("Geronimo01", "StrongPass123", "Geronimo", "Rodriguez", "user@example.com", "+57", v);
    }

    private static Set<String> invalidFields(RegisterRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    // ---------------------------------------------------------
    // Happy path
    // ---------------------------------------------------------

    @Test
    void validRequestHasNoViolations() {
        assertThat(invalidFields(valid())).isEmpty();
    }

    @Test
    @DisplayName("email is trimmed and lowercased")
    void emailIsNormalized() {
        RegisterRequest r = build("Geronimo01", "StrongPass123", "Geronimo", "Rodriguez",
                "  User@Example.COM ", "+57", "3001234567");

        assertThat(r.email()).isEqualTo("user@example.com");
    }

    // ---------------------------------------------------------
    // Username
    // ---------------------------------------------------------

    @ParameterizedTest(name = "[{0}] is a valid username")
    @ValueSource(strings = {"a", "MimiRandomS", "Abc!#$%&*()_", "A1b2C3d4E5f6", "user.name-1", "~"})
    void validUsernames(String username) {
        assertThat(invalidFields(withUsername(username))).doesNotContain("username");
    }

    @ParameterizedTest(name = "[{0}] is rejected")
    @ValueSource(strings = {"", " ", "has space", "1234567890123", "ñandú", "bob@x.com", "@", "tab\there"})
    @DisplayName("invalid usernames: blank, spaces, >12 chars, non-ASCII, '@'")
    void invalidUsernames(String username) {
        assertThat(invalidFields(withUsername(username))).contains("username");
    }

    // ---------------------------------------------------------
    // Password
    // ---------------------------------------------------------

    @ParameterizedTest(name = "length {0} is accepted")
    @ValueSource(ints = {8, 72})
    void passwordBoundariesAreAccepted(int length) {
        assertThat(invalidFields(withPassword("a".repeat(length)))).doesNotContain("password");
    }

    @ParameterizedTest(name = "length {0} is rejected")
    @ValueSource(ints = {0, 7, 73})
    void passwordOutOfRangeIsRejected(int length) {
        assertThat(invalidFields(withPassword("a".repeat(length)))).contains("password");
    }

    // ---------------------------------------------------------
    // Names
    // ---------------------------------------------------------

    @Test
    void nameBoundariesAreAccepted() {
        assertThat(invalidFields(withNames("a".repeat(58), "b".repeat(34)))).isEmpty();
    }

    @Test
    void namesOverTheLimitAreRejected() {
        assertThat(invalidFields(withNames("a".repeat(59), "b".repeat(35))))
                .contains("firstName", "lastName");
    }

    @Test
    void blankNamesAreRejected() {
        assertThat(invalidFields(withNames(" ", ""))).contains("firstName", "lastName");
    }

    // ---------------------------------------------------------
    // Phone (must follow the same rules as PhoneVerificationRequest)
    // ---------------------------------------------------------

    @ParameterizedTest(name = "[{0}] is a valid country code")
    @ValueSource(strings = {"+1", "+57", "+593"})
    void validCountryCodes(String code) {
        assertThat(invalidFields(withCountryCode(code))).doesNotContain("phoneCountryCode");
    }

    @ParameterizedTest(name = "[{0}] is rejected as country code")
    @ValueSource(strings = {"", "57", "+0", "+", "abc", "+5 7", "+1234"})
    void invalidCountryCodes(String code) {
        assertThat(invalidFields(withCountryCode(code))).contains("phoneCountryCode");
    }

    @ParameterizedTest(name = "[{0}] is a valid phone number")
    @ValueSource(strings = {"1", "3001234567", "123456789012345"})
    void validPhoneNumbers(String number) {
        assertThat(invalidFields(withPhone(number))).doesNotContain("phoneNumber");
    }

    @ParameterizedTest(name = "[{0}] is rejected as phone number")
    @ValueSource(strings = {"", "abc", "300 123 4567", "300-123", "+573001234567", "1234567890123456"})
    void invalidPhoneNumbers(String number) {
        assertThat(invalidFields(withPhone(number))).contains("phoneNumber");
    }
}