package be.yannickdeturck.lagomshop.item.impl;

import be.yannickdeturck.lagomshop.item.api.AddItemResponse;
import be.yannickdeturck.lagomshop.item.api.Item;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class ItemEntity extends PersistentEntity<ItemCommand, ItemEvent, ItemState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemEntity.class);

    @Override
    public Behavior initialBehavior(Optional<ItemState> snapshotState) {
        LOGGER.info("Setting up initialBehaviour with snapshotState = {}", snapshotState);
        BehaviorBuilder b = newBehaviorBuilder(snapshotState.orElse(
                ItemState.of(Optional.empty(), LocalDateTime.now()))
        );

        // Register command handler
        b.setCommandHandler(AddItem.class, (cmd, ctx) -> {
            if (state().getItem().isPresent()) {
                ctx.invalidCommand(String.format("Item %s is already created", entityId()));
                return ctx.done();
            } else {
                Item item = Item.of(UUID.fromString(entityId()), cmd.getAddItemRequest().getName(),
                        cmd.getAddItemRequest().getPrice());
                final ItemAdded itemAdded = ItemAdded.builder().item(item).build();
                LOGGER.info("Processed AddItem command into ItemAdded event {}", itemAdded);
                return ctx.thenPersist(itemAdded, evt ->
                        ctx.reply(AddItemResponse.of(itemAdded.getItem().getId())));
            }
        });

        // Register event handler
        b.setEventHandler(ItemAdded.class, evt -> {
                    LOGGER.info("Processed ItemAdded event, updated item state");
                    return state().withItem(evt.getItem())
                            .withTimestamp(LocalDateTime.now());
                }
        );

        // Register read-only handler eg a handler that doesn't result in events being created
        b.setReadOnlyCommandHandler(GetItem.class,
                (cmd, ctx) -> {
                    LOGGER.info("Processed GetItem command, returned item");
                    ctx.reply(GetItemReply.of(state().getItem()));
                }
        );

        return b.build();
    }
}
