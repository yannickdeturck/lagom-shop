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

    public static final Logger LOGGER = LoggerFactory.getLogger(OrderEntity.class);

    @Override
    public Behavior initialBehavior(Optional<OrderState> snapshotState) {
        LOGGER.info("snapshotState = {}", snapshotState);
        BehaviorBuilder b = newBehaviorBuilder(snapshotState.orElse(
                OrderState.of(Optional.empty(), LocalDateTime.now()))
        );

        b.setCommandHandler(CreateOrder.class, (cmd, ctx) -> {
            if (state().getOrder().isPresent()) {
                ctx.invalidCommand(String.format("Order %s is already created", entityId()));
                return ctx.done();
            } else {
                CreateOrderRequest orderRequest = cmd.getCreateOrderRequest();
                Order order = Order.of(UUID.fromString(entityId()), orderRequest.getItemId(), orderRequest.getAmount(),
                        orderRequest.getCustomer());
                final OrderCreated orderCreated = OrderCreated.builder().order(order).build();
                LOGGER.info("OrderCreated: {}", orderCreated);
                return ctx.thenPersist(orderCreated, evt ->
                        ctx.reply(CreateOrderResponse.of(orderCreated.getOrder().getId())));
            }
        });

        b.setEventHandler(OrderCreated.class,
                evt -> state().withOrder(evt.getOrder())
                        .withTimestamp(LocalDateTime.now())
        );

        b.setReadOnlyCommandHandler(GetOrder.class,
                (cmd, ctx) -> ctx.reply(GetOrderReply.of(state().getOrder()))
        );

        return b.build();
    }
}
