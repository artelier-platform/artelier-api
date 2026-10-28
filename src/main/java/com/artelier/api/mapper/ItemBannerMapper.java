package com.artelier.api.mapper;

import com.artelier.api.dto.response.ItemBannerResponse;
import com.artelier.api.entity.ItemBanner;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemBannerMapper {

    ItemBannerResponse toResponse(ItemBanner itemBanner);
}