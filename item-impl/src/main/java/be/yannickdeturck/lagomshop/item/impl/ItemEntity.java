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

    public static final Logger LOGGER = LoggerFactory.getLogger(ItemEntity.class);

    @Override
    public Behavior initialBehavior(Optional<ItemState> snapshotState) {
        LOGGER.info("snapshotState = {}", snapshotState);
        BehaviorBuilder b = newBehaviorBuilder(snapshotState.orElse(
                ItemState.of(Optional.empty(), LocalDateTime.now()))
        );

        b.setCommandHandler(AddItem.class, (cmd, ctx) -> {
// TODO don't think this is needed since command is validated during creation...
//            if (StringUtils.isEmpty(cmd.getItem().getName())) {
//                ctx.invalidCommand("Name must be defined");
//                return ctx.done();
//            }
//            if (cmd.getItem().getPrice() == null) {
//                ctx.invalidCommand("Price must be defined");
//                return ctx.done();
//            }

            Item item = Item.of(UUID.fromString(entityId()), cmd.getAddItemRequest().getName(),
                    cmd.getAddItemRequest().getPrice());
            final ItemAdded itemAdded = ItemAdded.builder().item(item).build();
            LOGGER.info("itemAdded: {}", itemAdded);
            return ctx.thenPersist(itemAdded, evt -> ctx.reply(AddItemResponse.of(itemAdded.getItem().getId())));

        });

        b.setEventHandler(ItemAdded.class,
                evt -> state().withItem(evt.getItem())
                        .withTimestamp(LocalDateTime.now())
        );

        // This could be used instead of querying Cassandra directly
//        b.setReadOnlyCommandHandler(GetItem.class,
//                (cmd, ctx) -> ctx.reply(GetItemReply.of(state().getItem()))
//        );

        return b.build();
    }
}
