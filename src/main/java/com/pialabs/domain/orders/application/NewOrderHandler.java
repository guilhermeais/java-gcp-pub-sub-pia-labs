package com.pialabs.domain.orders.application;

import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.pialabs.domain.orders.core.models.Order;
import com.pialabs.domain.orders.core.ports.incoming.SaveOrder;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewOrderHandler {

  private boolean isJUnitTest = System.getProperty("org.junit.jupiter") != null;

  private final String projectId;
  private final String subscriptionId;
  private final SaveOrder saveOrder;

  private Subscriber subscriber;

  private final Gson gson = new GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .registerTypeAdapter(
      Date.class,
      (JsonDeserializer<Date>) (json, typeOfT, context) -> {
        try {
          return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .parse(json.getAsString());
        } catch (ParseException e) {
          throw new JsonParseException(e);
        }
      }
    )
    .create();

  public NewOrderHandler(SaveOrder saveOrder) {
    this.projectId = System.getenv("PROJECT_ID");
    this.subscriptionId = System.getenv("SUBSCRIPTION_ID");

    if (projectId == null || projectId.isEmpty()) {
      throw new IllegalArgumentException(
        "PROJECT_ID environment variable must be set."
      );
    }

    if (subscriptionId == null || subscriptionId.isEmpty()) {
      throw new IllegalArgumentException(
        "SUBSCRIPTION_ID environment variable must be set."
      );
    }

    this.saveOrder = saveOrder;
  }

  public void start() {
    ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(
      projectId,
      subscriptionId
    );
    MessageReceiver receiver = createMessageReceiver();

    try {
      subscriber = startSubscriber(subscriptionName, receiver);
      waitForMessages(subscriber, subscriptionName);
    } catch (Exception e) {
      System.out.println("Error occurred while subscribing: " + e.getMessage());
    }
  }

  public void stop() {
    if (subscriber != null) {
      subscriber.stopAsync().awaitTerminated();

      System.out.println("Subscriber stopped.");

      subscriber = null;
    }
  }

  private MessageReceiver createMessageReceiver() {
    return (message, consumer) -> {
      System.out.println("Id: " + message.getMessageId());
      System.out.println("Data: " + message.getData().toStringUtf8());

      String json = message.getData().toStringUtf8();
      System.out.println("Json: " + json);

      try {
        Order order = gson.fromJson(json, Order.class);
        saveOrder.handle(order);
      } catch (JsonSyntaxException e) {
        System.out.println(
          "Error occurred while parsing JSON: " + e.getMessage()
        );

        e.printStackTrace();
      } catch (Exception e) {
        System.out.println(
          "Error occurred while saving order: " + e.getMessage()
        );
        e.printStackTrace();
      } finally {
        consumer.ack();
      }
    };
  }

  private Subscriber startSubscriber(
    ProjectSubscriptionName subscriptionName,
    MessageReceiver receiver
  ) {
    System.out.println("Starting subscriber...");
    System.out.println("IsTesting: " + isJUnitTest);

    Subscriber subscriber = Subscriber
      .newBuilder(subscriptionName, receiver)
      .build();

    subscriber.startAsync().awaitRunning();
    return subscriber;
  }

  private void waitForMessages(
    Subscriber subscriber,
    ProjectSubscriptionName subscriptionName
  ) {
    System.out.printf(
      "Listening for messages on %s:\n",
      subscriptionName.toString()
    );
    while (subscriber.isRunning()) {}
  }
}
