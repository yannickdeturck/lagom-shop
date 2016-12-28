package be.yannickdeturck.lagomshop.order.impl;

import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Yannick De Turck
 */
public class OrderEntity extends PersistentEntity<OrderCommand, OrderEvent, OrderState> {

    public static final Logger logger = LoggerFactory.getLogger(OrderEntity.class);

    @Override
    public Behavior initialBehavior(Optional<OrderState> snapshotState) {
        logger.info("Setting up initialBehaviour with snapshotState = {}", snapshotState);
        BehaviorBuilder b = newBehaviorBuilder(snapshotState.orElse(
                OrderState.of(Optional.empty(), LocalDateTime.now()))
        );

        // Register command handler
        b.setCommandHandler(CreateOrder.class, (cmd, ctx) -> {
            if (state().getOrder().isPresent()) {
                ctx.invalidCommand(String.format("Order %s is already created", entityId()));
                return ctx.done();
            } else {
                CreateOrderRequest orderRequest = cmd.getCreateOrderRequest();
                Order order = Order.of(UUID.fromString(entityId()), orderRequest.getItemId(), orderRequest.getAmount(),
                        orderRequest.getCustomer());
                final OrderCreated orderCreated = OrderCreated.builder().order(order).build();
                logger.info("Processed CreateOrder command into OrderCreated event {}", orderCreated);
                return ctx.thenPersist(orderCreated, evt ->
                        ctx.reply(CreateOrderResponse.of(orderCreated.getOrder().getId())));
            }
        });

        // Register event handler
        b.setEventHandler(OrderCreated.class, evt -> {
                    logger.info("Processed OrderCreated event, updated order state");
                    return state().withOrder(evt.getOrder())
                            .withTimestamp(LocalDateTime.now());
                }
        );

        // Register read-only handler eg a handler that doesn't result in events being created
        b.setReadOnlyCommandHandler(GetOrder.class,
                (cmd, ctx) -> {
                    logger.info("Processed GetOrder command, returned order");
                    ctx.reply(GetOrderReply.of(state().getOrder()));
                }
        );

        return b.build();
    }
}
