package com.pialabs.domain.orders.infraestructure;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pialabs.domain.orders.core.models.Category;
import com.pialabs.domain.orders.core.models.Customer;
import com.pialabs.domain.orders.core.models.FetchOrdersCommand;
import com.pialabs.domain.orders.core.models.Item;
import com.pialabs.domain.orders.core.models.Order;
import com.pialabs.domain.orders.core.models.Sku;
import com.pialabs.domain.orders.core.ports.outgoing.OrdersRepository;

public class PostgresOrdersRepository implements OrdersRepository {

    @Override
    public void saveOrder(Order order) throws SQLException {
        Connection conn = null;
        try {
            System.out.println("Getting connection with db...");
            conn = DatabaseConnectionPool.getConnection();
            System.out.println("Connected to db...");

            conn.setAutoCommit(false);

            upsertCostumer(conn, order.getCustomer());

            String sql = "INSERT INTO orders (uuid, created_at, processed_at, type, customer_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, order.getUuid());
            pstmt.setTimestamp(2, new java.sql.Timestamp(order.getCreatedAt().getTime()));
            pstmt.setTimestamp(3, new java.sql.Timestamp(order.getProcessedAt().getTime()));
            pstmt.setString(4, order.getType());
            pstmt.setInt(5, order.getCustomer().getId());
            System.out.println(
                    "Sending SQL: " + pstmt.toString());
            pstmt.executeUpdate();

            saveItems(conn, order.getItems(), order.getUuid());

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.out.println("Error on rollback: " + ex.getMessage());
                }
            }

            System.out.println("Error on saving order " + order.getUuid() + ": " + e.getMessage());

            throw e;
        } finally {
            if (conn != null) {
                try {
                    if (!conn.isClosed())
                        conn.close();
                } catch (SQLException e) {
                    System.out.println("Error on closing connection: " + e.getMessage());
                }
            }
        }
    }

    private void upsertCostumer(Connection conn, Customer customer) throws SQLException {
        try {
            System.out.println("Upserting customer " + customer.getId() + "...");
            String sql = "INSERT INTO customers (id, name) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET name = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, customer.getId());
            pstmt.setString(2, customer.getName());
            pstmt.setString(3, customer.getName());
            pstmt.executeUpdate();

            System.out.println("Customer " + customer.getId() + " upserted!");
        } catch (Exception e) {
            System.out.println("Error on upserting customer " + customer.getId() + ": " + e.getMessage());
            throw e;
        }
    }

    private void saveItems(Connection conn, List<Item> items, String orderUuid) throws SQLException {
        try {
            System.out.println("Saving items for order " + orderUuid + "...");

            String sql = "INSERT INTO items (id, order_uuid, product_id, quantity, category_id) VALUES (?, ?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (Item item : items) {
                upsertSku(conn, item.getSku());
                upsertCategory(conn, item.getCategory());
                pstmt.setInt(1, item.getId());
                pstmt.setString(2, orderUuid);
                pstmt.setString(3, item.getSku().getId());
                pstmt.setInt(4, item.getQuantity().intValue());
                pstmt.setString(5, item.getCategory().getId());

                pstmt.addBatch();
            }

            pstmt.executeBatch();

            System.out.println("Items for order " + orderUuid + " saved!");
        } catch (Exception e) {
            System.out.println("Error on saving items for order " + orderUuid + ": " + e.getMessage());
            throw e;
        }
    }

    private void upsertSku(Connection conn, Sku sku) throws SQLException {
        try {
            System.out.println("Upserting sku " + sku.getId() + "...");

            String sql = "INSERT INTO products (id, value) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET value = ?";

            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, sku.getId());
            pstmt.setBigDecimal(2, sku.getValue());
            pstmt.setBigDecimal(3, sku.getValue());
            pstmt.executeUpdate();

            System.out.println("Sku " + sku.getId() + " upserted!");
        } catch (Exception e) {
            System.out.println("Error on upserting sku " + sku.getId() + ": " + e.getMessage());
            throw e;
        }
    }

    private void upsertCategory(Connection conn, Category category) throws SQLException {
        try {
            System.out.println("Upserting category " + category.getId() + "...");

            String sql = "INSERT INTO categories (id, sub_category_id) VALUES (?, ?) ON CONFLICT (id) DO NOTHING";

            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, category.getId());
            if (category.getSubCategory() != null) {
                System.out.println("Upserting subcategory " + category.getSubCategory().getId() + "...");
                upsertCategory(conn, category.getSubCategory());
                pstmt.setString(2, category.getSubCategory().getId());
            } else {
                pstmt.setString(2, null);
            }

            pstmt.executeUpdate();

            System.out.println("Category " + category.getId() + " upserted!");
        } catch (Exception e) {
            System.out.println("Error on upserting category " + category.getId() + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Order> fetchOrders(FetchOrdersCommand fetchOrdersCommand) throws SQLException {
        try {
            System.out.println("[PostgresOrdersRepository.fetchOrders] Fetching orders...");
            Connection conn = DatabaseConnectionPool.getConnection();
            String sql = "SELECT o.uuid, o.created_at, o.processed_at, o.type, c.id as customer_id, c.name as customer_name, i.product_id, p.value, i.quantity, i.category_id, cat.sub_category_id "
                    +
                    "FROM orders o " +
                    "JOIN customers c ON o.customer_id = c.id " +
                    "JOIN items i ON o.uuid = i.order_uuid " +
                    "JOIN products p ON i.product_id = p.id " +
                    "JOIN categories cat ON i.category_id = cat.id " +
                    "WHERE 1=1";

            if (fetchOrdersCommand.getUuid() != null) {
                sql += " AND o.uuid = ?";
            }

            if (fetchOrdersCommand.getCostumerId() != null) {
                sql += " AND c.id = ?";
            }

            if (fetchOrdersCommand.getProductId() != null) {
                sql += " AND i.product_id = ?";
            }

            PreparedStatement pstmt = conn.prepareStatement(sql);

            Integer index = 1;
            if (fetchOrdersCommand.getUuid() != null) {
                pstmt.setString(index++, fetchOrdersCommand.getUuid());
            }

            if (fetchOrdersCommand.getCostumerId() != null) {
                pstmt.setInt(index++, fetchOrdersCommand.getCostumerId());
            }

            if (fetchOrdersCommand.getProductId() != null) {
                pstmt.setString(index++, fetchOrdersCommand.getProductId());
            }

            System.out.println("[PostgresOrdersRepository.fetchOrders] Query: " + pstmt.toString());
            ResultSet rs = pstmt.executeQuery();

            Map<String, Order> ordersMap = new HashMap<>();
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                System.out.println("[PostgresOrdersRepository.fetchOrders] Orders fetched: " + uuid);
                Order order = ordersMap.get(uuid);
                if (order == null) {
                    order = Order.builder().uuid(uuid).createdAt(rs.getTimestamp("created_at"))
                            .processedAt(rs.getTimestamp("processed_at"))
                            .type(rs.getString("type"))
                            .customer(new Customer(rs.getInt("customer_id"), rs.getString("customer_name")))
                            .items(new ArrayList<Item>())
                            .build();

                    System.out.println("[PostgresOrdersRepository.fetchOrders] Order created: " + order.getUuid());
                }

                Item item = new Item();
                item.setSku(
                        Sku
                                .builder()
                                .id(rs.getString("product_id"))
                                .value(rs.getBigDecimal("value"))
                                .build());

                item.setQuantity(rs.getInt("quantity"));
                item.setCategory(
                        Category
                                .builder()
                                .id(rs.getString("category_id"))
                                .subCategory(Category.builder().id(rs.getString("sub_category_id")).build())
                                .build());

                order.getItems().add(item);

                ordersMap.put(uuid, order);

                System.out.println("[PostgresOrdersRepository.fetchOrders] Orders fetched: " + ordersMap.size());
            }

            System.out.println("[PostgresOrdersRepository.fetchOrders] Orders fetched...");

            return new ArrayList<Order>(ordersMap.values());
        } catch (SQLException e) {
            System.out.println("[PostgresOrdersRepository.fetchOrders] Error on fetching orders: " + e.getMessage());
            throw e;
        }
    }
}