package com.artelier.api.integration.wompi.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardPaymentMethod.class, name = "CARD"),
        @JsonSubTypes.Type(value = NequiPaymentMethod.class, name = "NEQUI"),
        @JsonSubTypes.Type(value = PsePaymentMethod.class, name = "PSE")
})
@Schema(
        description = "Payment method information.",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "CARD", schema = CardPaymentMethod.class),
                @DiscriminatorMapping(value = "NEQUI", schema = NequiPaymentMethod.class),
                @DiscriminatorMapping(value = "PSE", schema = PsePaymentMethod.class)
        }
)
public interface PaymentMethodBody {
}