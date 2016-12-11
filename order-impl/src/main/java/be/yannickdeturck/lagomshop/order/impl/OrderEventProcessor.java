package be.yannickdeturck.lagomshop.order.impl;

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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author Yannick De Turck
 */
public class OrderEventProcessor extends ReadSideProcessor<OrderEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventProcessor.class);

    private final CassandraSession session;
    private final CassandraReadSide readSide;

    private PreparedStatement writeOrder = null; // initialized in prepare

    @Inject
    public OrderEventProcessor(CassandraSession session, CassandraReadSide readSide) {
        this.session = session;
        this.readSide = readSide;
    }

    private void setWriteOrder(PreparedStatement writeOrder) {
        this.writeOrder = writeOrder;
    }

    private CompletionStage<Done> prepareCreateTables(CassandraSession session) {
        LOGGER.info("Creating Cassandra tables...");
        return session.executeCreateTable(
                "CREATE TABLE IF NOT EXISTS item_order ("
                        + "orderId uuid, itemId uuid, amount int, customer text, PRIMARY KEY (orderId))");
    }

    private CompletionStage<Done> prepareWriteOrder(CassandraSession session) {
        LOGGER.info("Inserting into read-side table item_order...");
        return session.prepare("INSERT INTO item_order (orderId, itemId, amount, customer) VALUES (?, ?, ?, ?)")
                .thenApply(ps -> {
                    setWriteOrder(ps);
                    return Done.getInstance();
                });
    }

    /**
     * Write a persistent event into the read-side optimized database.
     */
    private CompletionStage<List<BoundStatement>> processOrderCreated(OrderCreated event) {
        BoundStatement bindWriteOrder = writeOrder.bind();
        bindWriteOrder.setUUID("orderId", event.getOrder().getId());
        bindWriteOrder.setUUID("itemId", event.getOrder().getItemId());
        bindWriteOrder.setInt("amount", event.getOrder().getAmount());
        bindWriteOrder.setString("customer", event.getOrder().getCustomer());
        LOGGER.info("Persisted Order {}", event.getOrder());
        return CassandraReadSide.completedStatements(Collections.singletonList(bindWriteOrder));
    }

    @Override
    public ReadSideHandler<OrderEvent> buildHandler() {
        CassandraReadSide.ReadSideHandlerBuilder<OrderEvent> builder = readSide.builder("item_order_offset");
        builder.setGlobalPrepare(() -> prepareCreateTables(session));
        builder.setPrepare(tag -> prepareWriteOrder(session));
        LOGGER.info("Setting up read-side event handlers...");
        builder.setEventHandler(OrderCreated.class, this::processOrderCreated);
        return builder.build();
    }

    @Override
    public PSequence<AggregateEventTag<OrderEvent>> aggregateTags() {
        return TreePVector.singleton(OrderEventTag.INSTANCE);
    }
}
