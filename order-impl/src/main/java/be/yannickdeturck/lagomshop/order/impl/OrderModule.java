package be.yannickdeturck.lagomshop.order.impl;

import be.yannickdeturck.lagomshop.item.api.ItemService;
import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;

public class OrderModule extends AbstractModule implements ServiceGuiceSupport {
    @Override
    protected void configure() {
        bindServices(serviceBinding(
                OrderService.class, OrderServiceImpl.class));
        bindClient(ItemService.class);
    }
}
