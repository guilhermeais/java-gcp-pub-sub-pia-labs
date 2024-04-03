package com.pialabs.domain.orders.core.ports.incoming;

import com.pialabs.domain.orders.core.models.Order;

public interface SaveOrder {
    void handle(Order command) throws Exception;
}
