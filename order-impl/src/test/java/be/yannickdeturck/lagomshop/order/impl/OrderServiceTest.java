package be.yannickdeturck.lagomshop.order.impl;

import akka.NotUsed;
import be.yannickdeturck.lagomshop.item.api.Item;
import be.yannickdeturck.lagomshop.item.api.ItemService;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.pcollections.PSequence;
import play.inject.Bindings;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Yannick De Turck
 */
public class OrderServiceTest {
    private static ServiceTest.TestServer server;

    private static ItemService itemService = Mockito.mock(ItemService.class);

    @BeforeClass
    public static void setUp() {
        server = ServiceTest.startServer(ServiceTest.defaultSetup()
                .withCassandra(true)
                .withConfigureBuilder(b -> b.overrides(
                        Bindings.bind(ItemService.class).toInstance(itemService))));
    }

    @AfterClass
    public static void tearDown() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Test
    public void createOrder_Should_GenerateId() throws Exception {
        // given
        UUID itemId = UUID.randomUUID();
        Mockito.when(itemService.getItem(itemId.toString()))
                .thenReturn(req -> CompletableFuture.completedFuture(
                        Item.of(itemId, "Chair", BigDecimal.valueOf(14.99))));
        OrderService service = server.client(OrderService.class);
        CreateOrderRequest createOrderRequest = CreateOrderRequest.builder()
                .itemId(itemId)
                .amount(2)
                .customer("Yannick")
                .build();

        // when
        CreateOrderResponse response = service.createOrder().invoke(createOrderRequest)
                .toCompletableFuture().get(3, SECONDS);

        // then
        Assert.assertNotNull(response.getId());
    }

    @Test
    public void createOrderWithNonExistingItem_Should_ReturnError() throws Exception {
        // given
        UUID itemId = UUID.randomUUID();
        Mockito.when(itemService.getItem(itemId.toString()))
                .thenReturn(req -> CompletableFuture.completedFuture(
                        null));
        OrderService service = server.client(OrderService.class);
        CreateOrderRequest createOrderRequest = CreateOrderRequest.builder()
                .itemId(itemId)
                .amount(2)
                .customer("Yannick")
                .build();

        // when
        try {
            service.createOrder().invoke(createOrderRequest).toCompletableFuture().get(3, SECONDS);
            Assert.fail("createOrder should've returned an error");
        } catch (Exception e) {
            // then
            Assert.assertEquals(String.format("com.lightbend.lagom.javadsl.api.deser.DeserializationException: " +
                    "No item found for id %s " +
                    "(TransportErrorCode{http=400, webSocket=1003, description='Unsupported Data/Bad Request'})",
                    itemId.toString()), e.getMessage());
        }
    }

    @Test
    public void getOrder_Should_ReturnCreatedOrder() throws Exception {
        // given
        UUID itemId = UUID.randomUUID();
        Mockito.when(itemService.getItem(itemId.toString()))
                .thenReturn(req -> CompletableFuture.completedFuture(
                        Item.of(itemId, "Chair", BigDecimal.valueOf(14.99))));
        OrderService service = server.client(OrderService.class);
        CreateOrderRequest createOrderRequest = CreateOrderRequest.builder()
                .itemId(itemId)
                .amount(2)
                .customer("Yannick")
                .build();
        CreateOrderResponse createOrderResponse = service.createOrder().invoke(createOrderRequest)
                .toCompletableFuture().get(3, SECONDS);

        // when
        ServiceTest.eventually(FiniteDuration.create(10, SECONDS), FiniteDuration.create(1000, MILLISECONDS), () -> {
            Order response = service.getOrder(createOrderResponse.getId().toString())
                    .invoke(NotUsed.getInstance()).toCompletableFuture().get(3, SECONDS);

            // then
            Assert.assertEquals(createOrderResponse.getId(), response.getId());
            Assert.assertEquals(itemId, response.getItemId());
            Assert.assertEquals(2, response.getAmount().intValue());
            Assert.assertEquals("Yannick", response.getCustomer());
        });
    }

    @Test
    public void getOrder_Should_ReturnErrorForNonExistingOrder() throws Exception {
        // given
        OrderService service = server.client(OrderService.class);

        // when
        UUID randomId = UUID.randomUUID();
        try {
            service.getOrder(randomId.toString())
                    .invoke(NotUsed.getInstance()).toCompletableFuture().get(3, SECONDS);
            Assert.fail("getOrder should've returned an error");
        } catch (Exception e) {
            // then
            Assert.assertEquals(String.format("com.lightbend.lagom.javadsl.api.transport.NotFound: " +
                            "No order found for id %s " +
                            "(TransportErrorCode{http=404, webSocket=1008, description='Policy Violation'})",
                    randomId.toString()), e.getMessage());
        }
    }

    @Test
    public void getAllOrders_Should_ReturnCreatedOrders() throws Exception {
        // given
        OrderService service = server.client(OrderService.class);
        UUID itemId = UUID.randomUUID();
        Mockito.when(itemService.getItem(itemId.toString()))
                .thenReturn(req -> CompletableFuture.completedFuture(
                        Item.of(itemId, "Chair", BigDecimal.valueOf(14.99))));
        CreateOrderRequest createOrderRequest = CreateOrderRequest.builder()
                .itemId(itemId)
                .amount(2)
                .customer("Yannick")
                .build();
        CreateOrderResponse createOrderResponse = service.createOrder().invoke(createOrderRequest)
                .toCompletableFuture().get(3, SECONDS);
        CreateOrderRequest createOrderRequest2 = CreateOrderRequest.builder()
                .itemId(itemId)
                .amount(5)
                .customer("John")
                .build();
        CreateOrderResponse createOrderResponse2 = service.createOrder().invoke(createOrderRequest2)
                .toCompletableFuture().get(3, SECONDS);

        // when
        ServiceTest.eventually(FiniteDuration.create(10, SECONDS), FiniteDuration.create(1000, MILLISECONDS), () -> {
            PSequence<Order> response = service.getAllOrders()
                    .invoke(NotUsed.getInstance()).toCompletableFuture().get(3, SECONDS);

            // then
            // TODO find a way to truncate Cassandra tables and check on size
//            Assert.assertEquals(2, response.size());
            Assert.assertTrue(String.format("Doesn't contain order %s", createOrderResponse.getId()),
                    response.stream().anyMatch(i -> createOrderResponse.getId().equals(i.getId())));
            Assert.assertTrue(String.format("Doesn't contain order %s", createOrderResponse2.getId()),
                    response.stream().anyMatch(i -> createOrderResponse2.getId().equals(i.getId())));
            response.stream()
                    .filter(i -> i.getId().equals(createOrderResponse.getId()))
                    .forEach(i -> {
                                Assert.assertEquals(createOrderResponse.getId(), i.getId());
                                Assert.assertEquals(itemId, i.getItemId());
                                Assert.assertEquals(2, i.getAmount().intValue());
                                Assert.assertEquals("Yannick", i.getCustomer());
                            }
                    );
            response.stream()
                    .filter(i -> i.getId().equals(createOrderResponse2.getId()))
                    .forEach(i -> {
                                Assert.assertEquals(createOrderResponse2.getId(), i.getId());
                                Assert.assertEquals(itemId, i.getItemId());
                                Assert.assertEquals(5, i.getAmount().intValue());
                                Assert.assertEquals("John", i.getCustomer());
                            }
                    );


        });
    }
}
