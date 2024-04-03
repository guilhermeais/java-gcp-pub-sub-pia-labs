package com.pialabs.domain.orders.core;

import java.util.List;

import com.pialabs.domain.orders.core.models.FetchOrdersCommand;
import com.pialabs.domain.orders.core.models.Order;
import com.pialabs.domain.orders.core.ports.incoming.FetchOrders;
import com.pialabs.domain.orders.core.ports.incoming.SaveOrder;
import com.pialabs.domain.orders.core.ports.outgoing.OrdersRepository;

public class OrdersFacade implements SaveOrder, FetchOrders {
    private final OrdersRepository orderRepository;

    public OrdersFacade(OrdersRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void handle(Order order) throws Exception {
        try {
            System.out.println("[OrdersFacade] Saving order " + order.getUuid());
            orderRepository.saveOrder(order);
            System.out.println("[OrdersFacade] Order " + order.getUuid() + " saved!");
        } catch (Exception e) {
            System.out.println("[OrdersFacade] Error on saving order " + order.getUuid() + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Order> handle(FetchOrdersCommand command) throws Exception {
        try {
            System.out.println("[OrdersFacade] Fetching orders...");
            List<Order> orders = orderRepository.fetchOrders(command);
            System.out.println("[OrdersFacade] Orders fetched: " + orders.size());
            return orders;
        } catch (Exception e) {
            throw e;
        }
    }

}
