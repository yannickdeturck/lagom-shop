package be.yannickdeturck.lagomshop.order.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.javadsl.immutable.ImmutableStyle;
import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.serialization.Jsonable;
import org.immutables.value.Value;

import java.time.Instant;

/**
 * @author Yannick De Turck
 */
public interface OrderEvent extends Jsonable, AggregateEvent<OrderEvent> {

    @Value.Immutable
    @ImmutableStyle
    @JsonDeserialize
    interface AbstractOrderCreated extends OrderEvent {
        @Override
        default AggregateEventTag<OrderEvent> aggregateTag() {
            return OrderEventTag.INSTANCE;
        }

        @Value.Parameter
        Order getOrder();

        @Value.Default
        default Instant getTimestamp() {
            return Instant.now();
        }
    }
}
