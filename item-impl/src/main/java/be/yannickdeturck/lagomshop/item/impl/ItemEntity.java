package be.yannickdeturck.lagomshop.item.impl;

import be.yannickdeturck.lagomshop.item.api.CreateItemResponse;
import be.yannickdeturck.lagomshop.item.api.Item;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class ItemEntity extends PersistentEntity<ItemCommand, ItemEvent, ItemState> {

    private static final Logger logger = LoggerFactory.getLogger(ItemEntity.class);

    @Override
    public Behavior initialBehavior(Optional<ItemState> snapshotState) {
        logger.info("Setting up initialBehaviour with snapshotState = {}", snapshotState);
        BehaviorBuilder b = newBehaviorBuilder(snapshotState.orElse(
                ItemState.of(Optional.empty(), LocalDateTime.now()))
        );

        // Register command handler
        b.setCommandHandler(CreateItem.class, (cmd, ctx) -> {
            if (state().getItem().isPresent()) {
                ctx.invalidCommand(String.format("Item %s is already created", entityId()));
                return ctx.done();
            } else {
                Item item = Item.of(UUID.fromString(entityId()), cmd.getCreateItemRequest().getName(),
                        cmd.getCreateItemRequest().getPrice());
                final ItemCreated itemCreated = ItemCreated.builder().item(item).build();
                logger.info("Processed CreateItem command into ItemCreated event {}", itemCreated);
                return ctx.thenPersist(itemCreated, evt ->
                        ctx.reply(CreateItemResponse.of(itemCreated.getItem().getId())));
            }
        });

        // Register event handler
        b.setEventHandler(ItemCreated.class, evt -> {
                    logger.info("Processed ItemCreated event, updated item state");
                    return state().withItem(evt.getItem())
                            .withTimestamp(LocalDateTime.now());
                }
        );

        // Register read-only handler eg a handler that doesn't result in events being created
        b.setReadOnlyCommandHandler(GetItem.class,
                (cmd, ctx) -> {
                    logger.info("Processed GetItem command, returned item");
                    ctx.reply(GetItemReply.of(state().getItem()));
                }
        );

        return b.build();
    }
}
