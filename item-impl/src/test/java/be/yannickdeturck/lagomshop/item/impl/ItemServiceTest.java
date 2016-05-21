package be.yannickdeturck.lagomshop.item.impl;

import akka.NotUsed;
import be.yannickdeturck.lagomshop.item.api.AddItemRequest;
import be.yannickdeturck.lagomshop.item.api.AddItemResponse;
import be.yannickdeturck.lagomshop.item.api.Item;
import be.yannickdeturck.lagomshop.item.api.ItemService;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.math.BigDecimal;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ItemServiceTest {
    private static ServiceTest.TestServer server;

    @BeforeClass
    public static void setUp() {
        server = ServiceTest.startServer(ServiceTest.defaultSetup());
    }

    @AfterClass
    public static void tearDown() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Test
    public void addItemShouldGenerateId() throws Exception {
        // given
        ItemService service = server.client(ItemService.class);
        AddItemRequest addItemRequest = AddItemRequest.builder()
                .name("Chair")
                .price(new BigDecimal(10.00))
                .build();

        // when
        AddItemResponse response = service.createItem().invoke(addItemRequest).toCompletableFuture().get(3, SECONDS);

        // then
        Assert.assertNotNull(response.getId());
    }
}
