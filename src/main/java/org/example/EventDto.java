package org.example;

import java.util.UUID;

public record EventDto(UUID eventId, String eventName, int numberOfTickets, String email) {
}
