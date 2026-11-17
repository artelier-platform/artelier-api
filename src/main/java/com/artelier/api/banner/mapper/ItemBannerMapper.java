package com.artelier.api.banner.mapper;

import com.artelier.api.banner.dto.ItemBannerResponse;
import com.artelier.api.banner.entity.ItemBanner;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemBannerMapper {

    ItemBannerResponse toResponse(ItemBanner itemBanner);
}