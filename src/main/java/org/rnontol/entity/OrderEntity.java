package org.rnontol.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity extends PanacheEntity {
    public Long clientId;
    public String description;
    public String state;
}
