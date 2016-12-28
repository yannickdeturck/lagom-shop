package be.yannickdeturck.lagomshop.item.impl;

import akka.NotUsed;
import akka.japi.Pair;
import be.yannickdeturck.lagomshop.item.api.CreateItemRequest;
import be.yannickdeturck.lagomshop.item.api.CreateItemResponse;
import be.yannickdeturck.lagomshop.item.api.Item;
import be.yannickdeturck.lagomshop.item.api.ItemService;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.Offset;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
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

public class ItemServiceImpl implements ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemServiceImpl.class);

    private final PersistentEntityRegistry persistentEntities;
    private final CassandraSession db;

    @Inject
    public ItemServiceImpl(PersistentEntityRegistry persistentEntities, ReadSide readSide,
                           CassandraSession db) {
        this.persistentEntities = persistentEntities;
        this.db = db;

        persistentEntities.register(ItemEntity.class);
        readSide.register(ItemEventProcessor.class);
    }

    @Override
    public ServiceCall<NotUsed, Item> getItem(String id) {
        return (req) -> {
            return persistentEntities.refFor(ItemEntity.class, id)
                    .ask(GetItem.of()).thenApply(reply -> {
                        logger.info(String.format("Looking up item %s", id));
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
            logger.info("Looking up all items");
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
    public ServiceCall<CreateItemRequest, CreateItemResponse> createItem() {
        return request -> {
            logger.info("Creating item: {}", request);
            UUID uuid = UUID.randomUUID();
            return persistentEntities.refFor(ItemEntity.class, uuid.toString())
                    .ask(CreateItem.of(request));
        };
    }

    @Override
    public Topic<be.yannickdeturck.lagomshop.item.api.ItemEvent> createdItemsTopic() {
        return TopicProducer.singleStreamWithOffset(offset -> {
            return persistentEntities
                    .eventStream(ItemEventTag.INSTANCE, offset)
                    .filter(eventOffSet -> eventOffSet.first() instanceof ItemCreated)
                    .map(this::convertItem);
        });
    }

    private Pair<be.yannickdeturck.lagomshop.item.api.ItemEvent, Offset> convertItem(Pair<ItemEvent, Offset> pair) {
        Item item = ((ItemCreated)pair.first()).getItem();
        logger.info("Converting ItemEvent" + item);
        return new Pair<>(new be.yannickdeturck.lagomshop.item.api.ItemEvent.ItemCreated(item.getId(), item.getName(),
                item.getPrice()), pair.second());
    }
}
