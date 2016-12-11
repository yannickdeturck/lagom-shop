package be.yannickdeturck.lagomshop.item.impl;

import akka.Done;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import org.pcollections.PSequence;
import org.pcollections.TreePVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author Yannick De Turck
 */
public class ItemEventProcessor extends ReadSideProcessor<ItemEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemEventProcessor.class);

    private final CassandraSession session;
    private final CassandraReadSide readSide;

    private PreparedStatement writeItem = null; // initialized in prepare

    @Inject
    public ItemEventProcessor(CassandraSession session, CassandraReadSide readSide) {
        this.session = session;
        this.readSide = readSide;
    }

    private void setWriteItem(PreparedStatement writeItem) {
        this.writeItem = writeItem;
    }

    private CompletionStage<Done> prepareCreateTables(CassandraSession session) {
        LOGGER.info("Creating Cassandra tables...");
        return session.executeCreateTable(
                "CREATE TABLE IF NOT EXISTS item ("
                        + "itemId uuid, name text, price decimal, PRIMARY KEY (itemId))");
    }

    private CompletionStage<Done> prepareWriteItem(CassandraSession session) {
        LOGGER.info("Inserting into read-side table item...");
        return session.prepare("INSERT INTO item (itemId, name, price) VALUES (?, ?, ?)")
                .thenApply(ps -> {
                    setWriteItem(ps);
                    return Done.getInstance();
                });
    }

    /**
     * Write a persistent event into the read-side optimized database.
     */
    private CompletionStage<List<BoundStatement>> processItemCreated(ItemCreated event) {
        BoundStatement bindWriteItem = writeItem.bind();
        bindWriteItem.setUUID("itemId", event.getItem().getId());
        bindWriteItem.setString("name", event.getItem().getName());
        bindWriteItem.setDecimal("price", event.getItem().getPrice());
        LOGGER.info("Persisted Item {}", event.getItem());
        return CassandraReadSide.completedStatements(Arrays.asList(bindWriteItem));
    }

    @Override
    public ReadSideHandler<ItemEvent> buildHandler() {
        CassandraReadSide.ReadSideHandlerBuilder<ItemEvent> builder = readSide.builder("item_offset");
        builder.setGlobalPrepare(() -> prepareCreateTables(session));
        builder.setPrepare(tag -> prepareWriteItem(session));
        LOGGER.info("Setting up read-side event handlers...");
        builder.setEventHandler(ItemCreated.class, this::processItemCreated);
        return builder.build();
    }

    @Override
    public PSequence<AggregateEventTag<ItemEvent>> aggregateTags() {
        return TreePVector.singleton(ItemEventTag.INSTANCE);
    }
}