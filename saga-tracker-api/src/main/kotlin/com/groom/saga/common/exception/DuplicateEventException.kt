package com.groom.saga.common.exception

class DuplicateEventException(eventId: String) : RuntimeException("Duplicate event: $eventId")
