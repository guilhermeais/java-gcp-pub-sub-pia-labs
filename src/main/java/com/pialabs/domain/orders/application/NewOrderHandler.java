package com.pialabs.domain.orders.application;

import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.JsonSyntaxException;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.pialabs.domain.orders.application.presenter.OrderPresenter;
import com.pialabs.domain.orders.core.models.Order;
import com.pialabs.domain.orders.core.ports.incoming.SaveOrder;

public class NewOrderHandler {

  private boolean isJUnitTest = System.getProperty("org.junit.jupiter") != null;

  private final String projectId;
  private final String subscriptionId;
  private final SaveOrder saveOrder;

  private Subscriber subscriber;

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

      try {
        Order order = OrderPresenter.fromJson(json);
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
