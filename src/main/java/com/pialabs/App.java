package com.pialabs;

import com.pialabs.domain.orders.application.FetchOrdersHttpHandler;
import com.pialabs.domain.orders.application.NewOrderHandler;
import com.pialabs.domain.orders.core.OrdersFacade;
import com.pialabs.domain.orders.core.ports.outgoing.OrdersRepository;
import com.pialabs.domain.orders.infraestructure.DatabaseConnectionPool;
import com.pialabs.domain.orders.infraestructure.PostgresOrdersRepository;

import static spark.Spark.*;

public class App {
	public static void main(String... args) throws Exception {

		OrdersRepository ordersRepository = new PostgresOrdersRepository();
		OrdersFacade ordersFacade = new OrdersFacade(ordersRepository);
		NewOrderHandler handler = new NewOrderHandler(ordersFacade);

		FetchOrdersHttpHandler fetchOrdersHttpHandler = new FetchOrdersHttpHandler(ordersFacade);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			DatabaseConnectionPool.close();
			handler.stop();
			stop();
		}));

		initExceptionHandler((e) -> System.out.println("Uh-oh"));

		port(8080);
		get("/orders", (req, res) -> fetchOrdersHttpHandler.handle(req, res));

		handler.start();
	}
}