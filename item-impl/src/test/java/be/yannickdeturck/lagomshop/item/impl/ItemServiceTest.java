package be.yannickdeturck.lagomshop.item.impl;

import akka.NotUsed;
import be.yannickdeturck.lagomshop.item.api.CreateItemRequest;
import be.yannickdeturck.lagomshop.item.api.CreateItemResponse;
import be.yannickdeturck.lagomshop.item.api.Item;
import be.yannickdeturck.lagomshop.item.api.ItemService;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pcollections.PSequence;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigDecimal;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Yannick De Turck
 */
public class ItemServiceTest {
    private static ServiceTest.TestServer server;

    @BeforeClass
    public static void setUp() {
        server = ServiceTest.startServer(ServiceTest.defaultSetup()
                .withCassandra(true));
    }

    @AfterClass
    public static void tearDown() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Test
    public void createItem_Should_GenerateId() throws Exception {
        // given
        ItemService service = server.client(ItemService.class);
        CreateItemRequest createItemRequest = CreateItemRequest.builder()
                .name("Chair")
                .price(new BigDecimal(10.00))
                .build();

        // when
        CreateItemResponse response = service.createItem().invoke(createItemRequest)
                .toCompletableFuture().get(3, SECONDS);

        // then
        Assert.assertNotNull(response.getId());
    }

    @Test
    public void getItem_Should_ReturnCreatedItem() throws Exception {
        // given
        ItemService service = server.client(ItemService.class);
        CreateItemRequest createItemRequest = CreateItemRequest.builder()
                .name("Chair")
                .price(new BigDecimal(10.00))
                .build();
        CreateItemResponse createItemResponse = service.createItem().invoke(createItemRequest).toCompletableFuture().get(3, SECONDS);

        // when
        // TODO disable circuit breaker to reduce time needed?
        // TODO should a 'get' service actually have a circuit breaker?
        ServiceTest.eventually(FiniteDuration.create(10, SECONDS), FiniteDuration.create(1000, MILLISECONDS), () -> {
            Item response = service.getItem(createItemResponse.getId().toString())
                    .invoke(NotUsed.getInstance()).toCompletableFuture().get(3, SECONDS);

            // then
            Assert.assertEquals(createItemResponse.getId(), response.getId());
            Assert.assertEquals("Chair", response.getName());
            Assert.assertEquals(new BigDecimal(10.00), response.getPrice());
        });
    }

    @Test
    public void getItem_Should_ReturnErrorForNonExistingItem() throws Exception {
        // given
        ItemService service = server.client(ItemService.class);

        // when
        UUID randomId = UUID.randomUUID();
        try {
            service.getItem(randomId.toString())
                    .invoke(NotUsed.getInstance()).toCompletableFuture().get(3, SECONDS);
            Assert.fail("getItem should've returned an error");
        } catch (Exception e) {
            // then
            Assert.assertEquals(String.format("com.lightbend.lagom.javadsl.api.transport.NotFound: " +
                            "No item found for id %s " +
                            "(TransportErrorCode{http=404, webSocket=1008, description='Policy Violation'})",
                    randomId.toString()), e.getMessage());
        }
    }

    @Test
    public void getAllItems_Should_ReturnCreatedItems() throws Exception {
        // given
        ItemService service = server.client(ItemService.class);
        CreateItemRequest createItemRequest = CreateItemRequest.builder()
                .name("Chair")
                .price(new BigDecimal(10.00))
                .build();
        CreateItemResponse createItemResponse = service.createItem().invoke(createItemRequest).toCompletableFuture().get(3, SECONDS);
        CreateItemRequest createItemRequest2 = CreateItemRequest.builder()
                .name("Table")
                .price(new BigDecimal(24.99))
                .build();
        CreateItemResponse createItemResponse2 = service.createItem().invoke(createItemRequest2).toCompletableFuture().get(3, SECONDS);

        // when
        ServiceTest.eventually(FiniteDuration.create(10, SECONDS), FiniteDuration.create(1000, MILLISECONDS), () -> {
            PSequence<Item> response = service.getAllItems()
                    .invoke(NotUsed.getInstance()).toCompletableFuture().get(3, SECONDS);

            // then
            // TODO find a way to truncate Cassandra tables and check on size
//            Assert.assertEquals(2, response.size());
            Assert.assertTrue(String.format("Doesn't contain item %s", createItemResponse.getId()),
                    response.stream().anyMatch(i -> createItemResponse.getId().equals(i.getId())));
            Assert.assertTrue(String.format("Doesn't contain item %s", createItemResponse2.getId()),
                    response.stream().anyMatch(i -> createItemResponse2.getId().equals(i.getId())));
            response.stream()
                    .filter(i -> i.getId().equals(createItemResponse.getId()))
                    .forEach(i -> {
                                Assert.assertEquals(createItemResponse.getId(), i.getId());
                                Assert.assertEquals("Chair", i.getName());
                                Assert.assertEquals(new BigDecimal(10.00), i.getPrice());
                            }
                    );
            response.stream()
                    .filter(i -> i.getId().equals(createItemResponse2.getId()))
                    .forEach(i -> {
                                Assert.assertEquals(createItemResponse2.getId(), i.getId());
                                Assert.assertEquals("Table", i.getName());
                                Assert.assertEquals(new BigDecimal(24.99), i.getPrice());
                            }
                    );


        });
    }
}
