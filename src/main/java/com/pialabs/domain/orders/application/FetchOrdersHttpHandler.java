package com.pialabs.domain.orders.application;

import com.pialabs.domain.orders.application.presenter.OrderPresenter;
import com.pialabs.domain.orders.core.models.FetchOrdersCommand;
import com.pialabs.domain.orders.core.models.Order;
import com.pialabs.domain.orders.core.ports.incoming.FetchOrders;
import java.util.List;
import spark.Request;
import spark.Response;

public class FetchOrdersHttpHandler {

  private final FetchOrders fetchOrders;

  public FetchOrdersHttpHandler(FetchOrders fetchOrders) {
    this.fetchOrders = fetchOrders;
  }

  public String handle(Request request, Response response) {
    System.out.println("[FetchOrdersHttpHandler] Fetching orders...");
    String uuid = request.queryParamsSafe("uuid");
    String productId = request.queryParamsSafe("product_id");
    String costumerIdStr = request.queryParamsSafe("costumer_id");
    Integer costumerId = null;

    if (costumerIdStr != null) {
      costumerId = Integer.parseInt(costumerIdStr);
    }

    FetchOrdersCommand command = FetchOrdersCommand
      .builder()
      .uuid(uuid)
      .costumerId(costumerId)
      .productId(productId)
      .build();

    System.out.println(
      "[FetchOrdersHttpHandler] Query: uuid=" +
      uuid +
      ", costumerId=" +
      costumerId +
      ", productId=" +
      productId
    );

    try {
      List<Order> orders = fetchOrders.handle(command);
      response.type("application/json");
      response.status(200);
      String res = OrderPresenter.toJson(orders);

      System.out.println("[FetchOrdersHttpHandler] Orders fetched: " + res);
      return res;
    } catch (Exception e) {
      System.out.println(
        "[FetchOrdersHttpHandler] Error on fetching orders: " + e.getMessage()
      );
      return e.getMessage();
    }
  }
}
