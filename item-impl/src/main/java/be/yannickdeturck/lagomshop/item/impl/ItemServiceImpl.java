package be.yannickdeturck.lagomshop.item.impl;

import akka.NotUsed;
import be.yannickdeturck.lagomshop.item.api.AddItemRequest;
import be.yannickdeturck.lagomshop.item.api.AddItemResponse;
import be.yannickdeturck.lagomshop.item.api.Item;
import be.yannickdeturck.lagomshop.item.api.ItemService;
import com.datastax.driver.core.Row;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRef;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import org.pcollections.PSequence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.pcollections.TreePVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ItemServiceImpl implements ItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemServiceImpl.class);

    private final PersistentEntityRegistry persistentEntities;
    private final CassandraSession db;
//    private final PubSubRegistry topics;


    @Inject
    public ItemServiceImpl(PersistentEntityRegistry persistentEntities, CassandraReadSide readSide,
//                           PubSubRegistry topics,
                           CassandraSession db) {
        this.persistentEntities = persistentEntities;
//        this.topics = topics;
        this.db = db;

        persistentEntities.register(ItemEntity.class);
        readSide.register(ItemEventProcessor.class);
    }

    // getItem implemented by querying in Cassandra (results in a delay due to having it being persisted first...)
//    @Override
//    public ServiceCall<NotUsed, Item> getItem(String id) {
//        return (req) -> {
//            CompletionStage<Item> result =
//                    db.selectOne("SELECT itemId, name, price FROM item WHERE itemId = ?", UUID.fromString(id))
//                            .thenApply(row -> {
//                                if (!row.isPresent()) {
//                                    throw new NotFound("No item found for id " + id);
//                                } else {
//                                    Row r = row.get();
//                                    return Item.of(r.getUUID("itemId"),
//                                            r.getString("name"),
//                                            r.getDecimal("price"));
//                                }
//                            });
//            return result;
//        };
//    }

    @Override
    public ServiceCall<NotUsed, Item> getItem(String id) {
        return (req) -> {
            return persistentEntities.refFor(ItemEntity.class, id)
                    .ask(GetItem.of()).thenApply(reply -> {
                if (reply.getItem().isPresent())
                    return reply.getItem().get();
                else
                    throw new NotFound(String.format("No item found for id %s", id));
            });
        };
    }

    @Override
    public ServiceCall<NotUsed, PSequence<Item>> getAllItems() {
        return (req) -> {
            CompletionStage<PSequence<Item>> result = db.selectAll("SELECT itemId, name, price FROM item")
                    .thenApply(rows -> {
                        List<Item> items = rows.stream().map(row -> Item.of(row.getUUID("itemId"),
                                row.getString("name"),
                                row.getDecimal("price"))).collect(Collectors.toList());
                        return TreePVector.from(items);
                    });
            return result;
        };
    }

    @Override
    public ServiceCall<AddItemRequest, AddItemResponse> createItem() {
        return request -> {
            // also add publish stuff here
            // Publish received entity into topic named "Topic"
//            PubSubRef<Item> topic = topics.refFor(TopicId.of(Item.class, "topic"));
//            topic.publish(request);
            LOGGER.info("createItem: {}.", request);
            // For now, generate the UUID server side
            UUID uuid = UUID.randomUUID();
            return persistentEntities.refFor(ItemEntity.class, uuid.toString())
                    .ask(AddItem.of(request));
        };
    }
}
