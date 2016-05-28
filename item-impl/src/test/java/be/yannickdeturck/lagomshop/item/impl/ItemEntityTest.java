package be.yannickdeturck.lagomshop.item.impl;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import be.yannickdeturck.lagomshop.item.api.AddItemRequest;
import be.yannickdeturck.lagomshop.item.api.AddItemResponse;
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
    public void addItem_Should_CreateItemAddedEvent() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());

        // when
        PersistentEntityTestDriver.Outcome<ItemEvent, ItemState> outcome = driver.run(
                AddItem.of(AddItemRequest.of("Chair", BigDecimal.valueOf(14.99))));

        // then
        Assert.assertTrue(outcome.getReplies().get(0) instanceof AddItemResponse);
        Assert.assertEquals(AddItemResponse.of(id), outcome.getReplies().get(0));
        ItemAdded itemAdded = ((ItemAdded) outcome.events().get(0));
        Assert.assertEquals(id, itemAdded.getItem().getId());
        Assert.assertEquals("Chair", itemAdded.getItem().getName());
        Assert.assertEquals(BigDecimal.valueOf(14.99), itemAdded.getItem().getPrice());
        Assert.assertNotNull(((ItemAdded) outcome.events().get(0)).getTimestamp());
        Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
    }

    @Test
    public void addExistingItem_Should_ThrowInvalidCommandException() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());
        driver.run(AddItem.of(AddItemRequest.of("Chair", BigDecimal.valueOf(14.99))));

        // when
        PersistentEntityTestDriver.Outcome<ItemEvent, ItemState> outcome = driver.run(
                AddItem.of(AddItemRequest.of("Chair2", BigDecimal.valueOf(14.99))));

        // then
        Assert.assertEquals(PersistentEntity.InvalidCommandException.class, outcome.getReplies().get(0).getClass());
        Assert.assertEquals(Collections.emptyList(), outcome.events());
        Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
    }

    @Test
    public void addItemWithoutName_Should_ThrowNullPointerException() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());

        // when
        try {
            driver.run(AddItem.of(AddItemRequest.of(null, BigDecimal.valueOf(14.99))));
            Assert.fail();
        } catch (NullPointerException e) {
            // then
            Assert.assertEquals("name", e.getMessage());
            Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
        }
    }

    @Test
    public void addItemWithoutPrice_Should_ThrowNullPointerException() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());

        // when
        try {
            driver.run(AddItem.of(AddItemRequest.of("Chair", null)));
            Assert.fail();
        } catch (NullPointerException e) {
            // then
            Assert.assertEquals("price", e.getMessage());
            Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
        }
    }

    @Test
    public void addItemWithNegativePrice_Should_ThrowSomeException() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<ItemCommand, ItemEvent, ItemState> driver = new PersistentEntityTestDriver<>(
                system, new ItemEntity(), id.toString());

        // when
        try {
            driver.run(AddItem.of(AddItemRequest.of("Chair", BigDecimal.valueOf(-14.99))));
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
        driver.run(AddItem.of(AddItemRequest.of("Chair", BigDecimal.valueOf(14.99))));
        Item chair = Item.of(id, "Chair", BigDecimal.valueOf(14.99));

        // when
        PersistentEntityTestDriver.Outcome<ItemEvent, ItemState> outcome = driver.run(GetItem.of());

        // then
        Assert.assertEquals(GetItemReply.of(Optional.of(chair)), outcome.getReplies().get(0));
        Assert.assertEquals(Collections.emptyList(), outcome.events());
        Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
    }
}
