package be.yannickdeturck.lagomshop.order.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.javadsl.immutable.ImmutableStyle;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.lightbend.lagom.serialization.Jsonable;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * @author Yannick De Turck
 */
public interface OrderCommand extends Jsonable {

    @Value.Immutable
    @ImmutableStyle
    @JsonDeserialize
    public interface AbstractCreateOrder extends OrderCommand, CompressedJsonable, PersistentEntity.ReplyType<CreateOrderResponse> {

        @Value.Parameter
        CreateOrderRequest getCreateOrderRequest();
    }

    @Value.Immutable(singleton = true)
    @ImmutableStyle
    @JsonDeserialize
    public interface AbstractGetOrder extends OrderCommand, CompressedJsonable, PersistentEntity.ReplyType<GetOrderReply> {

    }

    @Value.Immutable
    @ImmutableStyle
    @JsonDeserialize
    public interface AbstractGetOrderReply extends Jsonable {

        @Value.Parameter
        Optional<Order> getOrder();
    }
}
