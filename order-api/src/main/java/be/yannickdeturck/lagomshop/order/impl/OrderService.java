package be.yannickdeturck.lagomshop.order.impl;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.Method;
import org.pcollections.PSequence;

/**
 * @author Yannick De Turck
 */
public interface OrderService extends Service {
    /**
     * Example: curl http://localhost:9000/api/orders/5e59ff61-214c-461f-9e29-89de0cf88f90
     */
    ServiceCall<NotUsed, Order> getOrder(String id);

    /**
     * Example: curl http://localhost:9000/api/orders
     */
    ServiceCall<NotUsed, PSequence<Order>> getAllOrders();

    /**
     * Example:
     * curl -v -H "Content-Type: application/json" -X POST -d
     * '{"name": "Chair", "price": 10.50}' http://localhost:9000/api/orders
     */
     ServiceCall<CreateOrderRequest, CreateOrderResponse> createOrder();

    @Override
    default Descriptor descriptor() {
        return Service.named("orderservice").with(
                Service.restCall(Method.GET,  "/api/orders/:id", this::getOrder),
                Service.restCall(Method.GET,  "/api/orders", this::getAllOrders),
                Service.restCall(Method.POST, "/api/orders", this::createOrder)
        ).withAutoAcl(true);
    }
}
