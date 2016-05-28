package be.yannickdeturck.lagomshop.order.impl;

import akka.Done;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSideProcessor;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * @author Yannick De Turck
 */
public class OrderEventProcessor extends CassandraReadSideProcessor<OrderEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventProcessor.class);

    @Override
    public AggregateEventTag<OrderEvent> aggregateTag() {
        return OrderEventTag.INSTANCE;
    }


    private PreparedStatement writeOrder = null; // initialized in prepare
    private PreparedStatement writeOffset = null; // initialized in prepare

    private void setWriteOrder(PreparedStatement writeOrder) {
        this.writeOrder = writeOrder;
    }

    private void setWriteOffset(PreparedStatement writeOffset) {
        this.writeOffset = writeOffset;
    }

    /**
     * Prepare read-side table and statements
     */
    @Override
    public CompletionStage<Optional<UUID>> prepare(CassandraSession session) {
        return
                prepareCreateTables(session).thenCompose(a ->
                        prepareWriteOrder(session).thenCompose(b ->
                                prepareWriteOffset(session).thenCompose(c ->
                                        selectOffset(session))));
    }

    private CompletionStage<Done> prepareCreateTables(CassandraSession session) {
        return session.executeCreateTable(
                "CREATE TABLE IF NOT EXISTS item_order ("
                        + "orderId uuid, itemId uuid, amount int, customer text, PRIMARY KEY (orderId))")
                .thenCompose(a -> session.executeCreateTable(
                        "CREATE TABLE IF NOT EXISTS item_order_offset ("
                                + "partition int, offset timeuuid, PRIMARY KEY (partition))"));
    }

    private CompletionStage<Done> prepareWriteOrder(CassandraSession session) {
        return session.prepare("INSERT INTO item_order (orderId, itemId, amount, customer) VALUES (?, ?, ?, ?)").thenApply(ps -> {
            setWriteOrder(ps);
            return Done.getInstance();
        });
    }

    private CompletionStage<Done> prepareWriteOffset(CassandraSession session) {
        return session.prepare("INSERT INTO item_order_offset (partition, offset) VALUES (1, ?)").thenApply(ps -> {
            setWriteOffset(ps);
            return Done.getInstance();
        });
    }

    private CompletionStage<Optional<UUID>> selectOffset(CassandraSession session) {
        return session.selectOne("SELECT offset FROM item_order_offset")
                .thenApply(
                        optionalRow -> optionalRow.map(r -> r.getUUID("offset")));
    }

    /**
     * Bind the read side persistence to the OrderCreated event.
     */
    @Override
    public EventHandlers defineEventHandlers(EventHandlersBuilder builder) {
        builder.setEventHandler(OrderCreated.class, this::processOrderCreated);
        return builder.build();
    }

    /**
     * Write a persistent event into the read-side optimized database.
     */
    private CompletionStage<List<BoundStatement>> processOrderCreated(OrderCreated event, UUID offset) {
        BoundStatement bindWriteOrder = writeOrder.bind();
        bindWriteOrder.setUUID("orderId", event.getOrder().getId());
        bindWriteOrder.setUUID("itemId", event.getOrder().getItemId());
        bindWriteOrder.setInt("amount", event.getOrder().getAmount());
        bindWriteOrder.setString("customer", event.getOrder().getCustomer());
        BoundStatement bindWriteOffset = writeOffset.bind(offset);
        LOGGER.info("Persisted Order {}", event.getOrder());
        return completedStatements(Arrays.asList(bindWriteOrder, bindWriteOffset));
    }
}
