package be.yannickdeturck.lagomshop.item.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.javadsl.immutable.ImmutableStyle;
import com.lightbend.lagom.serialization.Jsonable;
import org.immutables.value.Value;

import java.util.UUID;

@Value.Immutable
@ImmutableStyle
@JsonDeserialize
public interface AbstractAddItemResponse extends Jsonable {

    @Value.Parameter
    UUID getId();
}
