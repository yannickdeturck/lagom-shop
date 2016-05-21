package be.yannickdeturck.lagomshop.order.impl;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import org.pcollections.PSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class OrderServiceImpl implements OrderService {

    // private final PersistentEntityRegistry persistentEntities;
    // private final CassandraSession db;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Inject
    public OrderServiceImpl() {
    // PersistentEntityRegistry persistentEntities, //CassandraReadSide readSide,
                            // CassandraSession db) {
        // this.persistentEntities = persistentEntities;
        // this.db = db;

        // persistentEntities.register(UserEntity.class);
//        readSide.register(UserEventProcessor.class);
    }

    @Override
    public ServiceCall<NotUsed, PSequence<Order>> getAllOrders() {
        return request -> {
          return null;
        };
    }

    // private PersistentEntityRef<UserCommand> userEntityRef(String userName) {
    //     PersistentEntityRef<UserCommand> ref = persistentEntities.refFor(UserEntity.class, userName);
    //     return ref;
    // }

//    @Override
//    public ServiceCall<String, NotUsed, User> getUser() {
//        return (id, request) -> {
//            User user = User.builder()
//                    .firstName(id)
//                    .build();
//            return CompletableFuture.completedFuture(user);
//        };
//    }
//
//    @Override
//    public ServiceCall<NotUsed, CreateUserRequest, User> createUser() {
//        return null;
//    }

//    @Override
//    public ServiceCall<NotUsed, Source<String, NotUsed>, Source<String, NotUsed>> stream() {
//        return (id, hellos) -> completedFuture(
//                hellos.mapAsync(8, name -> helloService.hello().invoke(name, NotUsed.getInstance())));
//    }
}
