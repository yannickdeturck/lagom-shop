package be.yannickdeturck.lagomshop.order.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.javadsl.immutable.ImmutableStyle;
import com.lightbend.lagom.serialization.Jsonable;
import org.immutables.value.Value;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * @author Yannick De Turck
 */
@Value.Immutable
@ImmutableStyle
@JsonDeserialize
public interface AbstractOrderState extends Jsonable {

    @Value.Parameter
    Optional<Order> getOrder();

    @Value.Parameter
    LocalDateTime getTimestamp();
}
