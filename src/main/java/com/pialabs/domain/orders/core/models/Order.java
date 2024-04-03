package com.pialabs.domain.orders.core.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    private String uuid;
    private Date createdAt;
    private Date processedAt = new Date();
    private String type;
    private Customer customer;
    private List<Item> items = new ArrayList<>();

    BigDecimal getTotal() {
        BigDecimal total = new BigDecimal(0);
        for (Item item : items) {
            total = total.add(item.getSku().getValue().multiply(new BigDecimal(item.getQuantity().intValue())));
        }
        return total;
    }

}