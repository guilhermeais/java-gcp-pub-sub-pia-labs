package com.pialabs.domain.orders.core.ports.outgoing;

import java.util.List;

import com.pialabs.domain.orders.core.models.FetchOrdersCommand;
import com.pialabs.domain.orders.core.models.Order;

public interface OrdersRepository {
    void saveOrder(Order order) throws Exception;

    List<Order> fetchOrders(
            FetchOrdersCommand fetchOrdersCommand) throws Exception;
}
