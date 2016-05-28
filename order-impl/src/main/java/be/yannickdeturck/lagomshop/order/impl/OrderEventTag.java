package be.yannickdeturck.lagomshop.order.impl;

import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;

/**
 * @author Yannick De Turck
 */
public class OrderEventTag {
    public static final AggregateEventTag<OrderEvent> INSTANCE =
            AggregateEventTag.of(OrderEvent.class);
}
