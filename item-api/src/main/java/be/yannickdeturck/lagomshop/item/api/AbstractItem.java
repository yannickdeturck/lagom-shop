package be.yannickdeturck.lagomshop.item.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.javadsl.immutable.ImmutableStyle;
import org.immutables.value.Value;
import java.math.BigDecimal;
import java.util.UUID;

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
}
