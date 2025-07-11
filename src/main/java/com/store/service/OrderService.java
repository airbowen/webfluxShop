package com.store.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.dto.OrderCreateRequestDTO;
import com.store.dto.OrderResponseDTO;
import com.store.entity.*;
import com.store.repository.*;
import com.store.util.RedisLockUtil;
import com.store.util.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.store.repository.UserRepository;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    // kafka topic name 下单成功后，通知其他系统
    private static final String ORDER_CREATED_TOPIC = "order-created";
    // kafka topic name 订单支付成功后，通知其他系统
    private static final String ORDER_PAID_TOPIC = "order-paid";
    // kafka topic name 订单取消后，通知其他系统
    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled";
    // 最大重试次数
    private static final int MAX_RETRY_COUNT = 3;
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderMessageRepository orderMessageRepository;
    // 雪花算法生成订单号 分布式id生成器, 生成唯一订单号
    // he Snowflake ID Generator ensures that even if you have multiple instances of your order service running, 
    // each will generate globally unique order codes without any coordination, making your system highly scalable and reliable
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final RedisLockUtil redisLockUtil;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    
    // declare 注入在应用启动阶段就会校验依赖是否齐全，若缺失 Bean，启动时即抛错。
    public OrderService(OrderRepository orderRepository,
                       OrderItemRepository orderItemRepository,
                       ProductRepository productRepository,
                       OrderMessageRepository orderMessageRepository,
                       SnowflakeIdGenerator snowflakeIdGenerator,
                       RedisLockUtil redisLockUtil,
                       KafkaTemplate<String, String> kafkaTemplate,
                       ObjectMapper objectMapper,
                       UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.orderMessageRepository = orderMessageRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.redisLockUtil = redisLockUtil;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }
    
    @Transactional
    public List<OrderResponseDTO> createOrder(OrderCreateRequestDTO request, Long userId) {
        logger.info("Creating order for user: {}", userId);
        
        // Validate request
        validateOrderRequest(request);
        
        // Group items by merchant
        Map<Long, List<OrderCreateRequestDTO.OrderItemDTO>> itemsByMerchant = groupItemsByMerchant(request.getItems());
        
        List<OrderResponseDTO> createdOrders = new ArrayList<>();
        
        // Create separate orders for each merchant
        // 遍历每个商家，创建订单, 汇总一起createOrder返回
        for (Map.Entry<Long, List<OrderCreateRequestDTO.OrderItemDTO>> entry : itemsByMerchant.entrySet()) {
            Long merchantId = entry.getKey();
            List<OrderCreateRequestDTO.OrderItemDTO> items = entry.getValue();
            
            OrderResponseDTO order = createOrderForMerchant(userId, merchantId, items);
            createdOrders.add(order);
        }
        
        return createdOrders;
    }
    // 校验请求参数,非空校验，商品是否存在，商品是否上架，商品数量是否大于0 
    private void validateOrderRequest(OrderCreateRequestDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty");
        }
        
        // Check if all products exist and are on sale
        List<Long> productIds = request.getItems().stream()
                .map(OrderCreateRequestDTO.OrderItemDTO::getProductId)
                .collect(Collectors.toList());
        
        List<ProductEntity> products = productRepository.findByIdsAndOnSale(productIds);
        if (products.size() != productIds.size()) {
            throw new IllegalArgumentException("Some products are not available for sale");
        }
        
        // Validate quantities
        for (OrderCreateRequestDTO.OrderItemDTO item : request.getItems()) {
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
        }
    }
    
    private Map<Long, List<OrderCreateRequestDTO.OrderItemDTO>> groupItemsByMerchant(List<OrderCreateRequestDTO.OrderItemDTO> items) {
        // Get product details to group by merchant
        List<Long> productIds = items.stream()
                .map(OrderCreateRequestDTO.OrderItemDTO::getProductId)
                .collect(Collectors.toList());
        
        List<ProductEntity> products = productRepository.findByIdsAndOnSale(productIds);
        Map<Long, ProductEntity> productMap = products.stream()
                .collect(Collectors.toMap(ProductEntity::getId, p -> p));
        
        return items.stream()
                .collect(Collectors.groupingBy(
                        item -> productMap.get(item.getProductId()).getMerchantId()
                ));
    }
    
    @Transactional
    public OrderResponseDTO createOrderForMerchant(Long userId, Long merchantId, List<OrderCreateRequestDTO.OrderItemDTO> items) {
        String orderCode = snowflakeIdGenerator.nextOrderCode();
        String lockKey = "order:lock:" + userId;
        String lockValue = UUID.randomUUID().toString();
        
        try {
            // Acquire distributed lock
            // 在高并发、多实例环境中，保证同一用户的“下单”操作不会被并行执行，避免库存错扣、重复下单等竞态问题
            if (!redisLockUtil.tryLock(lockKey, lockValue, 30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to acquire lock for user: " + userId);
            }
            
            // Check idempotency
            String idempotencyKey = "order:idempotency:" + userId + ":" + orderCode;
            if (!redisLockUtil.setIfAbsent(idempotencyKey, "1", 24, TimeUnit.HOURS)) {
                throw new RuntimeException("Duplicate order request detected");
            }
            
            // Calculate total amount and validate stock
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<ProductEntity> products = new ArrayList<>();
            
            for (OrderCreateRequestDTO.OrderItemDTO item : items) {
                ProductEntity product = productRepository.findByIdWithPessimisticLock(item.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));
                
                if (product.getStock() < item.getQuantity()) {
                    throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
                }
                
                // Update stock
                product.setStock(product.getStock() - item.getQuantity());
                products.add(product);
                
                // Calculate total
                totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            
            // Save updated products
            productRepository.saveAll(products);
            
            // Create order
            OrderEntity order = new OrderEntity(orderCode, userId, merchantId, totalAmount);
            order = orderRepository.save(order);
            
            // Create order items
            List<OrderItemEntity> orderItems = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                OrderCreateRequestDTO.OrderItemDTO item = items.get(i);
                ProductEntity product = products.get(i);
                
                OrderItemEntity orderItem = new OrderItemEntity(
                        order.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        product.getPrice()
                );
                orderItems.add(orderItem);
            }
            orderItemRepository.saveAll(orderItems);
            
            // Send Kafka message
            sendOrderCreatedMessage(order, orderItems);
            
            // Build response
            OrderResponseDTO response = buildOrderResponse(order, orderItems, products);
            
            logger.info("Order created successfully: {}", orderCode);
            return response;
            
        } finally {
            // Release lock
            redisLockUtil.releaseLock(lockKey, lockValue);
        }
    }
    
    private void sendOrderCreatedMessage(OrderEntity order, List<OrderItemEntity> orderItems) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("orderId", order.getId());
            message.put("orderCode", order.getOrderCode());
            message.put("userId", order.getUserId());
            message.put("merchantId", order.getMerchantId());
            message.put("totalAmount", order.getTotalAmount());
            message.put("status", order.getStatus());
            message.put("createTime", order.getCreateTime());
            message.put("items", orderItems);

            String payload = objectMapper.writeValueAsString(message);

            // Always log the message first as PENDING
            AtomicReference<OrderMessageEntity> msgEntityRef = new AtomicReference<>(new OrderMessageEntity(ORDER_CREATED_TOPIC, payload));
            msgEntityRef.get().setStatus("PENDING");
            msgEntityRef.set(orderMessageRepository.save(msgEntityRef.get()));

            kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getOrderCode(), payload)
                    .whenComplete((result, ex) -> {
                        OrderMessageEntity msgEntity = msgEntityRef.get();
                        if (ex == null) {
                            logger.info("Order created message sent successfully: {}", order.getOrderCode());
                            msgEntity.setStatus("SENT");
                        } else {
                            logger.error("Failed to send order created message: {}", ex.getMessage());
                            msgEntity.setStatus("FAILED");
                        }
                        msgEntity.setLastRetryTime(LocalDateTime.now());
                        orderMessageRepository.save(msgEntity);
                    });
        } catch (Exception e) {
            logger.error("Failed to serialize order message: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize order message", e);
        }
    }
    
    private void saveMessageForRetry(String topic, String payload) {
        OrderMessageEntity message = new OrderMessageEntity(topic, payload);
        message.setStatus("FAILED");
        orderMessageRepository.save(message);
    }
    
    private OrderResponseDTO buildOrderResponse(OrderEntity order, List<OrderItemEntity> orderItems, List<ProductEntity> products) {
        OrderResponseDTO response = new OrderResponseDTO(
                order.getId(),
                order.getOrderCode(),
                order.getUserId(),
                order.getMerchantId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreateTime()
        );
        
        // Set payTime and trackingNo from the order entity
        response.setPayTime(order.getPayTime());
        response.setTrackingNo(order.getTrackingNo());
        
        List<OrderResponseDTO.OrderItemResponseDTO> itemResponses = new ArrayList<>();
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItemEntity item = orderItems.get(i);
            ProductEntity product = products.get(i);
            
            OrderResponseDTO.OrderItemResponseDTO itemResponse = new OrderResponseDTO.OrderItemResponseDTO(
                    item.getId(),
                    item.getProductId(),
                    product.getName(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getRefundStatus()
            );
            itemResponses.add(itemResponse);
        }
        
        response.setItems(itemResponses);
        return response;
    }
    
    public OrderResponseDTO getOrderByCode(String orderCode) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderCode));
        
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(order.getId());
        
        // Get product details
        List<Long> productIds = orderItems.stream()
                .map(OrderItemEntity::getProductId)
                .collect(Collectors.toList());
        List<ProductEntity> products = productRepository.findAllById(productIds);
        Map<Long, ProductEntity> productMap = products.stream()
                .collect(Collectors.toMap(ProductEntity::getId, p -> p));
        
        return buildOrderResponse(order, orderItems, products);
    }
    
    public List<OrderResponseDTO> getOrdersByUser(Long userId) {
        List<OrderEntity> orders = orderRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return orders.stream()
                .map(order -> getOrderByCode(order.getOrderCode()))
                .collect(Collectors.toList());
    }

    public Long getUserIdByLoginName(String loginName) {
        return userRepository.findByLoginName(loginName)
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + loginName));
    }
} 