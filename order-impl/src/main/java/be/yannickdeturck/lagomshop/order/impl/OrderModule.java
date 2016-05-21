package be.yannickdeturck.lagomshop.order.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;

public class OrderModule extends AbstractModule implements ServiceGuiceSupport {
    @Override
    protected void configure() {
        bindServices(serviceBinding(
                OrderService.class, OrderServiceImpl.class));
        // bindClient(HelloService.class);
    }
}
