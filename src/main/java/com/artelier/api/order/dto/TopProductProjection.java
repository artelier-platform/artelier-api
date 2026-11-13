package com.artelier.api.order.dto;

import java.util.UUID;

public interface TopProductProjection {
    UUID getId();
    String getName();
    Long getTotalSold();
}