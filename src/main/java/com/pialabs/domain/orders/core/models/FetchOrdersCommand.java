package com.pialabs.domain.orders.core.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class FetchOrdersCommand {
    String uuid;
    Integer costumerId;
    String productId;
}
