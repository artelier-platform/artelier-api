package com.artelier.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BannerRequest(

        Boolean isActive,

        @NotNull
        @Size(min = 1, max = 20)
        List<@NotNull @Valid ItemBannerRequest> items

) {}