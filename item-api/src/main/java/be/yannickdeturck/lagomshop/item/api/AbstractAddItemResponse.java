package be.yannickdeturck.lagomshop.item.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.javadsl.immutable.ImmutableStyle;
import org.immutables.value.Value;

import java.util.UUID;

@Value.Immutable
@ImmutableStyle
@JsonDeserialize
public interface AbstractAddItemResponse {

    @Value.Parameter
    UUID getId();
}
