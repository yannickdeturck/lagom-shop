package be.yannickdeturck.lagomshop.order.impl;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Flow;
import be.yannickdeturck.lagomshop.item.api.Item;
import be.yannickdeturck.lagomshop.item.api.ItemService;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.deser.ExceptionMessage;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import com.lightbend.lagom.javadsl.pubsub.PubSubRef;
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry;
import com.lightbend.lagom.javadsl.pubsub.TopicId;
import org.pcollections.PSequence;
import org.pcollections.TreePVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    private final PubSubRegistry pubSubRegistry;

    @Inject
    public OrderServiceImpl(PersistentEntityRegistry persistentEntities, ReadSide readSide,
                            ItemService itemService, PubSubRegistry topics, CassandraSession db) {
        this.persistentEntities = persistentEntities;
        this.pubSubRegistry = topics;
        this.db = db;
        this.itemService = itemService;

        persistentEntities.register(OrderEntity.class);
        readSide.register(OrderEventProcessor.class);
        itemService.createdItemsTopic()
                .subscribe()
                .atLeastOnce(Flow.fromFunction((be.yannickdeturck.lagomshop.item.api.ItemEvent item) -> {
                    LOGGER.info("Subscriber: doing something with the created item " + item);
                    return Done.getInstance();
                }));
    }

    @Override
    public ServiceCall<NotUsed, Order> getOrder(String id) {
        return (req) -> {
            return persistentEntities.refFor(OrderEntity.class, id)
                    .ask(GetOrder.of()).thenApply(reply -> {
                        LOGGER.info(String.format("Looking up order %s", id));
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
            LOGGER.info("Looking up all orders");
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
            PubSubRef<CreateOrderRequest> topic = pubSubRegistry.refFor(TopicId.of(CreateOrderRequest.class, "topic"));
            topic.publish(request);
            CompletionStage<Item> response =
                    itemService.getItem(request.getItemId().toString()).invoke(NotUsed.getInstance());
            Item item = response.toCompletableFuture().join();
            if (item == null) {
                // TODO custom BadRequest Exception?
                throw new TransportException(TransportErrorCode.ProtocolError,
                        new ExceptionMessage("Bad Request", String.format("No item found for id %s",
                                request.getItemId().toString())));
            }
            LOGGER.info("Creating order {}", request);
            UUID uuid = UUID.randomUUID();
            return persistentEntities.refFor(OrderEntity.class, uuid.toString())
                    .ask(CreateOrder.of(request));
        };
    }

    public ServiceCall<NotUsed, Source<CreateOrderRequest, ?>> getLiveOrders() {
        return request -> {
            final PubSubRef<CreateOrderRequest> topic =
                    pubSubRegistry.refFor(TopicId.of(CreateOrderRequest.class, "topic"));
            return CompletableFuture.completedFuture(topic.subscriber());
        };
    }
}
