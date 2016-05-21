package be.yannickdeturck.lagomshop.order.impl;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.Method;
import org.pcollections.PSequence;

public interface OrderService extends Service {
    /**
     * Example: curl http://localhost:9000/api/orders
     */
    ServiceCall<NotUsed, PSequence<Order>> getAllOrders();

    /**
     * TODO fix: Example: curl -H "Content-Type: application/json" -X POST -d '{"firstName":
     * "Yannick", "lastName": "De Turck"}' http://localhost:9000/api/orders
     */
    // ServiceCall<Order, NotUsed> createOrder();

    @Override
    default Descriptor descriptor() {
        return Service.named("orderservice").with(
                Service.restCall(Method.GET,  "/api/orders", this::getAllOrders)
        ).withAutoAcl(true);
    }
}
