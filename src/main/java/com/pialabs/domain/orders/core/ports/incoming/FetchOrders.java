package com.pialabs.domain.orders.core.ports.incoming;

import java.util.List;

import com.pialabs.domain.orders.core.models.FetchOrdersCommand;
import com.pialabs.domain.orders.core.models.Order;

public interface FetchOrders {
    List<Order> handle(FetchOrdersCommand command) throws Exception;
}
