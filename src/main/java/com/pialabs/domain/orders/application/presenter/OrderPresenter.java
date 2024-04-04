package com.pialabs.domain.orders.application.presenter;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pialabs.domain.orders.core.models.Order;
import java.util.List;

public class OrderPresenter {

  private static final Gson gson = new GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .setDateFormat("yyyy-MM-dd HH:mm:ss")
    .create();

  public static String toJson(List<Order> orders) {
    JsonArray jsonOrders = new JsonArray();
    for (Order order : orders) {
      JsonObject jsonOrder = gson.toJsonTree(order).getAsJsonObject();
      jsonOrder.addProperty("total", order.getTotal());
      jsonOrders.add(jsonOrder);
    }
    return jsonOrders.toString();
  }

  public static Order fromJson(String json) {
    return gson.fromJson(json, Order.class);
  }
}
