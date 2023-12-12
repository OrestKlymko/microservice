package com.example.orderservice.service;


import com.example.orderservice.dto.InventoryResponse;
import com.example.orderservice.dto.OrderLineItemsDto;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.event.OrderPlacedEvent;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

	private final OrderRepository orderRepository;
	private final WebClient webClient;
	private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;

	public void placeOrder(OrderRequest orderRequest) {
		List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToDo).toList();
		Order order = Order.builder()
				.orderNumber(UUID.randomUUID().toString())
				.orderLineItemsList(orderLineItemsList)
				.build();

		List<String> skuCodesList = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuOrder).toList();


		InventoryResponse[] inventoryResponseArray = webClient.get()
				.uri("http://localhost:8084/api/inventory",
						uriBuilder -> uriBuilder.queryParam("skuCode", skuCodesList).build())
				.retrieve()
				.bodyToMono(InventoryResponse[].class)
				.block();


		if (inventoryResponseArray == null) throw new IllegalStateException("inventoryResponseArray is null");
		boolean allProductInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);
		if (allProductInStock) {
			orderRepository.save(order);
			kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
		} else {
			throw new IllegalArgumentException("Product is not at stock");
		}

	}

	private OrderLineItems mapToDo(OrderLineItemsDto order) {
		return OrderLineItems.builder()
				.skuOrder(order.getSkuOrder())
				.price(order.getPrice())
				.quantity(order.getQuantity())
				.build();
	}
}
