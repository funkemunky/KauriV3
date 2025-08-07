package dev.brighten.ac.handler.events;

import dev.brighten.ac.handler.MovementHandler;

public record ConfirmedPositionEvent(MovementHandler.TeleportAction action) {
}
