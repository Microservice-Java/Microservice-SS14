package com.storex.saga.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ChoreographyEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChoreographyEventPublisher.class);

    private final ApplicationEventPublisher eventPublisher;

    public ChoreographyEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(Object event) {
        log.info("Choreography EventBus -> Publishing event: {}", event.getClass().getSimpleName());
        eventPublisher.publishEvent(event);
    }
}
