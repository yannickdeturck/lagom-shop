package be.yannickdeturck.lagomshop.order.impl;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Yannick De Turck
 */
public class OrderEntityTest {
    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("OrderEntityTest");
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void createOrder_Should_CreateOrderCreatedEvent() {
        // given
        UUID id = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PersistentEntityTestDriver<OrderCommand, OrderEvent, OrderState> driver = new PersistentEntityTestDriver<>(
                system, new OrderEntity(), id.toString());

        // when
        PersistentEntityTestDriver.Outcome<OrderEvent, OrderState> outcome = driver.run(
                CreateOrder.of(CreateOrderRequest.of(itemId, 2, "Yannick")));

        // then
        Assert.assertTrue(outcome.getReplies().get(0) instanceof CreateOrderResponse);
        Assert.assertEquals(CreateOrderResponse.of(id), outcome.getReplies().get(0));
        OrderCreated orderCreated = ((OrderCreated) outcome.events().get(0));
        Assert.assertEquals(id, orderCreated.getOrder().getId());
        Assert.assertEquals(itemId, orderCreated.getOrder().getItemId());
        Assert.assertEquals(2, orderCreated.getOrder().getAmount().intValue());
        Assert.assertEquals("Yannick", orderCreated.getOrder().getCustomer());
        Assert.assertNotNull(((OrderCreated) outcome.events().get(0)).getTimestamp());
        Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
    }

    @Test
    public void createExistingOrder_Should_ThrowInvalidCommandException() {
        // given
        UUID id = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PersistentEntityTestDriver<OrderCommand, OrderEvent, OrderState> driver = new PersistentEntityTestDriver<>(
                system, new OrderEntity(), id.toString());
        driver.run(CreateOrder.of(CreateOrderRequest.of(itemId, 2, "Yannick")));

        // when
        PersistentEntityTestDriver.Outcome<OrderEvent, OrderState> outcome = driver.run(
                CreateOrder.of(CreateOrderRequest.of(itemId, 2, "Yannick")));

        // then
        Assert.assertEquals(PersistentEntity.InvalidCommandException.class, outcome.getReplies().get(0).getClass());
        Assert.assertEquals(Collections.emptyList(), outcome.events());
        Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
    }

    @Test
    public void createOrderWithoutItemId_Should_ThrowNullPointerException() {
        // given
        UUID id = UUID.randomUUID();
        PersistentEntityTestDriver<OrderCommand, OrderEvent, OrderState> driver = new PersistentEntityTestDriver<>(
                system, new OrderEntity(), id.toString());

        // when
        try {
            driver.run(CreateOrder.of(CreateOrderRequest.of(null, 2, "Yannick")));
            Assert.fail();
        } catch (NullPointerException e) {
            // then
            Assert.assertEquals("itemId", e.getMessage());
            Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
        }
    }

    @Test
    public void createOrderWithoutAmount_Should_ThrowNullPointerException() {
        // given
        UUID id = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PersistentEntityTestDriver<OrderCommand, OrderEvent, OrderState> driver = new PersistentEntityTestDriver<>(
                system, new OrderEntity(), id.toString());

        // when
        try {
            driver.run(CreateOrder.of(CreateOrderRequest.of(itemId, null, "Yannick")));
            Assert.fail();
        } catch (NullPointerException e) {
            // then
            Assert.assertEquals("amount", e.getMessage());
            Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
        }
    }

    @Test
    public void createOrderWithoutCustomer_Should_ThrowNullPointerException() {
        // given
        UUID id = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PersistentEntityTestDriver<OrderCommand, OrderEvent, OrderState> driver = new PersistentEntityTestDriver<>(
                system, new OrderEntity(), id.toString());

        // when
        try {
            driver.run(CreateOrder.of(CreateOrderRequest.of(itemId, 2, null)));
            Assert.fail();
        } catch (NullPointerException e) {
            // then
            Assert.assertEquals("customer", e.getMessage());
            Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
        }
    }

    @Test
    public void createOrderWithNegativeAmount_Should_ThrowSomeException() {
        // given
        UUID id = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PersistentEntityTestDriver<OrderCommand, OrderEvent, OrderState> driver = new PersistentEntityTestDriver<>(
                system, new OrderEntity(), id.toString());

        // when
        try {
            driver.run(CreateOrder.of(CreateOrderRequest.of(itemId, -2, "Yannick")));
            Assert.fail();
        } catch (IllegalStateException e) {
            // then
            Assert.assertEquals("Amount must be a positive value", e.getMessage());
            Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
        }
    }

    @Test
    public void getOrder_Should_ReturnGetOrderReply() {
        // given
        UUID id = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PersistentEntityTestDriver<OrderCommand, OrderEvent, OrderState> driver = new PersistentEntityTestDriver<>(
                system, new OrderEntity(), id.toString());
        driver.run(CreateOrder.of(CreateOrderRequest.of(itemId, 2, "Yannick")));
        Order order = Order.of(id, itemId, 2, "Yannick");

        // when
        PersistentEntityTestDriver.Outcome<OrderEvent, OrderState> outcome = driver.run(GetOrder.of());

        // then
        Assert.assertEquals(GetOrderReply.of(Optional.of(order)), outcome.getReplies().get(0));
        Assert.assertEquals(Collections.emptyList(), outcome.events());
        Assert.assertEquals(Collections.emptyList(), driver.getAllIssues());
    }
}
