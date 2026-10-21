package edu.arch.shop.order.service;

import edu.arch.shop.order.dto.OrderRequest;
import edu.arch.shop.order.dto.OrderResponse;
import edu.arch.shop.order.entity.Order;
import edu.arch.shop.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import edu.arch.shop.customer.entity.Customer;
import edu.arch.shop.customer.repository.CustomerRepository;
import edu.arch.shop.order.entity.OrderStatus;

import edu.arch.shop.notification.dto.NotificationRequest;
import edu.arch.shop.notification.service.NotificationService;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository, NotificationService notificationService) { // rozszerzenie konstruktora o repo customer i notification
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.notificationService = notificationService;
    }

    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse findById(Long id) {
        return toResponse(findOrder(id));
    }

    public List<OrderResponse> findByCustomerId(Long customerId) {
        findCustomer(customerId);

        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        validate(request);
        Order order = new Order(
                request.customerName(),
                request.productName(),
                request.quantity(),
                request.unitPrice()
        );
        if(request.customerId() != null){ // przypisanie klienta do zamówienia
            Customer customer = findCustomer(request.customerId());
            order.setCustomer(customer);
        }
        Order savedOrder = orderRepository.save(order);
        String message = "Produkt: " + savedOrder.getProductName() + ". Utworzono zamówienie numer " + savedOrder.getId() + " dla klienta " + savedOrder.getCustomerName();
        notificationService.create(new NotificationRequest(message));

        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        validate(request);
        Order order = findOrder(id);
        order.setCustomerName(request.customerName());
        order.setProductName(request.productName());
        order.setQuantity(request.quantity());
        order.setUnitPrice(request.unitPrice());
        if(request.customerId() != null){ // przypisanie zamówienia do nowego wskazanego klienta
            Customer customer = findCustomer(request.customerId());
            order.setCustomer(customer);
        }
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status){ // nowa metoda ustawia nowy status zamówienia
        if(status == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status is required");
        }
        Order order = findOrder(id);
        order.setStatus(status);

        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        Order order = findOrder(id);
        orderRepository.delete(order);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validate(OrderRequest request) {
        if (request.customerName() == null || request.customerName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required");
        }
        if (request.customerName().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is too long");
        }
        if (request.productName() == null || request.productName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required");
        }
        if (request.productName().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is too long");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive");
        }
        if (request.unitPrice() == null || request.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit price must be positive");
        }
        if (request.unitPrice().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit price can have at most 2 decimal places");
        }
        if (request.unitPrice().compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit price is too high");
        }
    }

    private OrderResponse toResponse(Order order) {
        BigDecimal totalPrice = order.getUnitPrice()
                .multiply(BigDecimal.valueOf(order.getQuantity()));
        Long customerId = order.getCustomer() != null ? order.getCustomer().getId() : null;
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                totalPrice,
                customerId,
                order.getStatus()
        );
    }

    private Customer findCustomer(Long customerId){ // nowa metoda "Znajdź customera po ID"
        return customerRepository.findById(customerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
}