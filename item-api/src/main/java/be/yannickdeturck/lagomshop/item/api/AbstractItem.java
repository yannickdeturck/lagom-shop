package be.yannickdeturck.lagomshop.item.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.javadsl.immutable.ImmutableStyle;
import org.immutables.value.Value;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Yannick De Turck
 */
@Value.Immutable
@ImmutableStyle
@JsonDeserialize
public interface AbstractItem {

    @Value.Parameter
    UUID getId();

    @Value.Parameter
    String getName();

    @Value.Parameter
    BigDecimal getPrice();

    @Value.Check
    default void check() {
        Preconditions.checkState(getPrice().signum() > 0, "Price must be a positive value");
    }
}
