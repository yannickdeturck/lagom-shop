package be.yannickdeturck.lagomshop.order.impl;

import akka.NotUsed;
import be.yannickdeturck.lagomshop.item.api.Item;
import be.yannickdeturck.lagomshop.item.api.ItemService;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.deser.ExceptionMessage;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import org.pcollections.PSequence;
import org.pcollections.TreePVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * @author Yannick De Turck
 */
public class OrderServiceImpl implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final PersistentEntityRegistry persistentEntities;
    private final CassandraSession db;
    private final ItemService itemService;

    @Inject
    public OrderServiceImpl(PersistentEntityRegistry persistentEntities, CassandraReadSide readSide,
                            ItemService itemService,
//                           PubSubRegistry topics,
                            CassandraSession db) {
        this.persistentEntities = persistentEntities;
//        this.topics = topics;
        this.db = db;
        this.itemService = itemService;

        persistentEntities.register(OrderEntity.class);
        readSide.register(OrderEventProcessor.class);
    }

    @Override
    public ServiceCall<NotUsed, Order> getOrder(String id) {
        return (req) -> {
            return persistentEntities.refFor(OrderEntity.class, id)
                    .ask(GetOrder.of()).thenApply(reply -> {
                        if (reply.getOrder().isPresent())
                            return reply.getOrder().get();
                        else
                            throw new NotFound(String.format("No order found for id %s", id));
                    });
        };
    }

    @Override
    public ServiceCall<NotUsed, PSequence<Order>> getAllOrders() {
        return (req) -> {
            CompletionStage<PSequence<Order>> result =
                    db.selectAll("SELECT orderId, itemId, amount, customer FROM item_order")
                            .thenApply(rows -> {
                                List<Order> items = rows.stream().map(row -> Order.of(
                                        row.getUUID("orderId"),
                                        row.getUUID("itemId"),
                                        row.getInt("amount"),
                                        row.getString("customer"))).collect(Collectors.toList());
                                return TreePVector.from(items);
                            });
            return result;
        };
    }

    @Override
    public ServiceCall<CreateOrderRequest, CreateOrderResponse> createOrder() {
        return request -> {
            // also add publish stuff here
            // Publish received entity into topic named "Topic"
//            PubSubRef<Order> topic = topics.refFor(TopicId.of(Order.class, "topic"));
//            topic.publish(request);
            CompletionStage<Item> response =
                    itemService.getItem(request.getItemId().toString()).invoke(NotUsed.getInstance());
            LOGGER.info("response -> " + response);
            Item item = response.toCompletableFuture().getNow(null);
            if (item == null) {
                // TODO custom BadRequest Exception?
                throw new TransportException(TransportErrorCode.ProtocolError,
                        new ExceptionMessage("Bad Request", String.format("No item found for id %s",
                                request.getItemId().toString())));
            }
            LOGGER.info("createOrder: {}.", request);
            UUID uuid = UUID.randomUUID();
            return persistentEntities.refFor(OrderEntity.class, uuid.toString())
                    .ask(CreateOrder.of(request));
        };
    }
}
