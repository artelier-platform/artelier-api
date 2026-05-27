package com.artelier.api.dto.projection;

import java.util.UUID;

public interface TopProductProjection {
    UUID getId();
    String getName();
    Long getTotalSold();
}