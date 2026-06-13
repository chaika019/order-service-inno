package by.chaika19.orderservice.controller;

import by.chaika19.orderservice.dto.OrderInputDto;
import by.chaika19.orderservice.dto.OrderOutputDto;
import by.chaika19.orderservice.model.OrderStatus;
import by.chaika19.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderOutputDto> createOrder(@Valid @RequestBody OrderInputDto orderInputDto) {

        OrderOutputDto created =orderService.createOrder(orderInputDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderOutputDto> getOrderById(@PathVariable Long id) {

        OrderOutputDto order = orderService.getOrderById((id));
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<Page<OrderOutputDto>> getOrdersFiltered(
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 15) Pageable pageable) {

        Page<OrderOutputDto> orders = orderService.getOrdersFiltered(statuses, startDate, endDate, pageable);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderOutputDto>> getOrdersByUserId(@PathVariable Long userId) {

        List<OrderOutputDto> orders = orderService.getOrdersByUserId(userId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderOutputDto> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderInputDto inputDto) {

        OrderOutputDto updated = orderService.updateOrder(id, inputDto);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderById(@PathVariable Long id) {

        orderService.deleteOrderById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
