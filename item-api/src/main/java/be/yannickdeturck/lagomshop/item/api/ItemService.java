package be.yannickdeturck.lagomshop.item.api;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.Method;
import org.pcollections.PSequence;

/**
 * @author Yannick De Turck
 */
public interface ItemService extends Service {

    /**
     * Example: curl http://localhost:9000/api/items/5e59ff61-214c-461f-9e29-89de0cf88f90
     */
    ServiceCall<NotUsed, Item> getItem(String id);

    /**
     * Example: curl http://localhost:9000/api/items
     */
    ServiceCall<NotUsed, PSequence<Item>> getAllItems();

    /**
     * Example:
     * curl -v -H "Content-Type: application/json" -X POST -d
     * '{"name": "Chair", "price": 10.50}' http://localhost:9000/api/items
     */
    ServiceCall<CreateItemRequest, CreateItemResponse> createItem();

    /**
     * Other useful URLs:
     *
     * http://localhost:8000/services                                  - Lists the available services
     * http://localhost:{SERVICE_PORT}/_status/circuit-breaker/current - Snapshot of current circuit breaker status
     * http://localhost:{SERVICE_PORT}/_status/circuit-breaker/stream  - Stream of circuit breaker status
     */

    @Override
    default Descriptor descriptor() {
        return Service.named("itemservice").withCalls(
                Service.restCall(Method.GET,  "/api/items/:id", this::getItem),
                Service.restCall(Method.GET,  "/api/items", this::getAllItems),
                Service.restCall(Method.POST, "/api/items", this::createItem)
        ).withAutoAcl(true);
    }
}
