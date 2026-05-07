package com.artelier.api.mapper;

import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "order.id", target = "orderId")
    PaymentResponse toResponse(Payment payment);
}