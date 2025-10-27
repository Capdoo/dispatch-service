package org.rnontol.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class OrderSnapshot extends PanacheEntity {
    public String description;
    public String state;
}
