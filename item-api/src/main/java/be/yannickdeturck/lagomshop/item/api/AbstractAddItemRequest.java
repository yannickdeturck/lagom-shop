package be.yannickdeturck.lagomshop.item.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.javadsl.immutable.ImmutableStyle;
import com.lightbend.lagom.serialization.Jsonable;
import org.immutables.value.Value;

import java.math.BigDecimal;

@Value.Immutable
@ImmutableStyle
@JsonDeserialize
public interface AbstractAddItemRequest extends Jsonable {

    @Value.Parameter
    String getName();

    @Value.Parameter
    BigDecimal getPrice();

    @Value.Check
    default void check() {
        Preconditions.checkState(getPrice().signum() > 0, "Price must be a positive value");
    }
}
