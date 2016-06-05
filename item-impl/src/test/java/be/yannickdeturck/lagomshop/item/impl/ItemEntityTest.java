package be.yannickdeturck.lagomshop.item.impl;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import be.yannickdeturck.lagomshop.item.api.CreateItemRequest;
import be.yannickdeturck.lagomshop.item.api.CreateItemResponse;
import be.yannickdeturck.lagomshop.item.api.Item;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Yannick De Turck
 */
public class ItemEntityTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("ItemEntityTest");
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void createItem_Should_CreateItemCreatedEvent() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());

        // when
        PersistentEntityTestDriver.Outcome<ItemEvent, ItemState> outcome = driver.run(
                CreateItem.of(CreateItemRequest.of("Chair", BigDecimal.valueOf(14.99))));

        // then
        Assert.assertTrue(outcome.getReplies().get(0) instanceof CreateItemResponse);
        Assert.assertEquals(CreateItemResponse.of(id), outcome.getReplies().get(0));
        ItemCreated itemCreated = ((ItemCreated) outcome.events().get(0));
        Assert.assertEquals(id, itemCreated.getItem().getId());
        Assert.assertEquals("Chair", itemCreated.getItem().getName());
        Assert.assertEquals(BigDecimal.valueOf(14.99), itemCreated.getItem().getPrice());
        Assert.assertNotNull(((ItemCreated) outcome.events().get(0)).getTimestamp());
        Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
    }

    @Test
    public void addExistingItem_Should_ThrowInvalidCommandException() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());
        driver.run(CreateItem.of(CreateItemRequest.of("Chair", BigDecimal.valueOf(14.99))));

        // when
        PersistentEntityTestDriver.Outcome<ItemEvent, ItemState> outcome = driver.run(
                CreateItem.of(CreateItemRequest.of("Chair2", BigDecimal.valueOf(14.99))));

        // then
        Assert.assertEquals(PersistentEntity.InvalidCommandException.class, outcome.getReplies().get(0).getClass());
        Assert.assertEquals(Collections.emptyList(), outcome.events());
        Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
    }

    @Test
    public void createItemWithoutName_Should_ThrowNullPointerException() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());

        // when
        try {
            driver.run(CreateItem.of(CreateItemRequest.of(null, BigDecimal.valueOf(14.99))));
            Assert.fail();
        } catch (NullPointerException e) {
            // then
            Assert.assertEquals("name", e.getMessage());
            Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
        }
    }

    @Test
    public void createItemWithoutPrice_Should_ThrowNullPointerException() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());

        // when
        try {
            driver.run(CreateItem.of(CreateItemRequest.of("Chair", null)));
            Assert.fail();
        } catch (NullPointerException e) {
            // then
            Assert.assertEquals("price", e.getMessage());
            Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
        }
    }

    @Test
    public void createItemWithNegativePrice_Should_ThrowSomeException() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());

        // when
        try {
            driver.run(CreateItem.of(CreateItemRequest.of("Chair", BigDecimal.valueOf(-14.99))));
            Assert.fail();
        } catch (IllegalStateException e) {
            // then
            Assert.assertEquals("Price must be a positive value", e.getMessage());
            Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
        }
    }

    @Test
    public void getItem_Should_ReturnGetItemReply() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());
        driver.run(CreateItem.of(CreateItemRequest.of("Chair", BigDecimal.valueOf(14.99))));
        Item chair = Item.of(id, "Chair", BigDecimal.valueOf(14.99));

        // when
        PersistentEntityTestDriver.Outcome<ItemEvent, ItemState> outcome = driver.run(GetItem.of());

        // then
        Assert.assertEquals(GetItemReply.of(Optional.of(chair)), outcome.getReplies().get(0));
        Assert.assertEquals(Collections.emptyList(), outcome.events());
        Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
    }
}
