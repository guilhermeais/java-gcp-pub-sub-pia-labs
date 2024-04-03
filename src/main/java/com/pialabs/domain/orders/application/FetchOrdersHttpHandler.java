package com.pialabs.domain.orders.application;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.pialabs.domain.orders.core.models.FetchOrdersCommand;
import com.pialabs.domain.orders.core.models.Order;
import com.pialabs.domain.orders.core.ports.incoming.FetchOrders;

import spark.Request;
import spark.Response;

public class FetchOrdersHttpHandler {
    private final FetchOrders fetchOrders;
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Date.class,
                    (JsonSerializer<Date>) (src, typeOfSrc,
                            context) -> new JsonPrimitive(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(src)))
            .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> {
                try {
                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(json.getAsString());
                } catch (ParseException e) {
                    throw new JsonParseException(e);
                }
            })
            .create();

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

        FetchOrdersCommand command = FetchOrdersCommand.builder()
                .uuid(uuid)
                .costumerId(costumerId)
                .productId(productId)
                .build();

        System.out
                .println("[FetchOrdersHttpHandler] Query: uuid=" + uuid + ", costumerId=" + costumerId + ", productId="
                        + productId);

        try {
            List<Order> orders = fetchOrders.handle(command);
            response.type("application/json");
            response.status(200);
            String res = gson.toJson(orders);

            System.out.println("[FetchOrdersHttpHandler] Orders fetched: " + res);
            return res;
        } catch (Exception e) {
            System.out.println("[FetchOrdersHttpHandler] Error on fetching orders: " + e.getMessage());
            return e.getMessage();
        }
    }
}