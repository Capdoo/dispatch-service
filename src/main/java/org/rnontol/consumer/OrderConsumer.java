package org.rnontol.consumer;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.rnontol.entity.OrderEntity;
import org.rnontol.entity.OrderSnapshot;
import io.quarkus.redis.client.RedisClient;

import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;


import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrderConsumer {

    private static final Logger LOG = Logger.getLogger(OrderConsumer.class);

    private final RedisAPI redis;

    public OrderConsumer(RedisAPI redis) {
        this.redis = redis;
    }

    @Incoming("orders")
    public Uni<Void> consume(String message) {

        LOG.infof("INICIO DE CONSUME DE MENSAJE: %s", message);

        // Guardar snapshot en Redis (clave única)
        Uni<Response> redisResult = redis.set(
                List.of("order_snapshot:" + UUID.randomUUID(), message)
        );
        // Persistir snapshot en PostgreSQL reactivo
        // Guardar en PostgreSQL reactivo
        OrderSnapshot snapshot = new OrderSnapshot();
        snapshot.description = message;
        snapshot.state = "DISPATCHED";

        JsonObject json = new JsonObject(message);
        Long orderId = json.getLong("id");

        // Actualizar estado en la BD
        Uni<Void> updateResult = Panache.withTransaction(() ->
                OrderEntity.<OrderEntity>findById(orderId)
                        .onItem().ifNotNull().invoke(order -> {
                            order.state = "DISPATCHED";
                        })
                        .flatMap(order -> order != null ? order.persistAndFlush().replaceWithVoid() : Uni.createFrom().voidItem())
        );

        return redisResult
                .flatMap(r -> updateResult)//para la bd
                .flatMap(r -> Panache.withTransaction(snapshot::persist))//para el redis
                .invoke(() -> LOG.info("Registro guardado en BD, actualizado y Redis"))
                .onFailure().invoke(t -> LOG.error("Error al procesar mensaje: " + t.getMessage()))
                .replaceWithVoid();
    }

}