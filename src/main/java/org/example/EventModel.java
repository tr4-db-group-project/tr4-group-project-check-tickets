package org.example;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "events", schema = "eventdb")
public class EventModel {
    public EventModel() {
    }

    public EventModel(UUID id, String name, int numberoftickets, String email) {
        this.id = id;
        this.name = name;
        this.numberoftickets = numberoftickets;
        this.email = email;
    }

    @Id
    UUID id;

    @Column(name = "name")
    String name;

    @Column(name = "numberoftickets")
    int numberoftickets;

    @Column(name = "email")
    String email;

}
