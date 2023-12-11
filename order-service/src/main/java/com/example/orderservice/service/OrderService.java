package com.example.orderservice.service;


import com.example.orderservice.dto.OrderLineItemsDto;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

	private final OrderRepository orderRepository;

	public void placeOrder(OrderRequest orderRequest){
		List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToDo).toList();
		Order orderMap = Order.builder()
				.orderNumber(UUID.randomUUID().toString())
				.orderLineItemsList(orderLineItemsList)
				.build();
		orderRepository.save(orderMap);
		log.info("Order saved");

	}

	private OrderLineItems mapToDo(OrderLineItemsDto order) {
		return OrderLineItems.builder()
				.skuOrder(order.getSkuOrder())
				.price(order.getPrice())
				.quantity(order.getQuantity())
				.build();
	}
}
