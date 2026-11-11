package in.chinche.hospital.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class AppointmentUpdatePublisher {

    private static final long STREAM_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private final Set<SseEmitter> adminEmitters = new CopyOnWriteArraySet<>();
    private final Set<SseEmitter> availabilityEmitters = new CopyOnWriteArraySet<>();

    public SseEmitter subscribeAdmin() {
        return subscribe(adminEmitters);
    }

    public SseEmitter subscribeAvailability() {
        return subscribe(availabilityEmitters);
    }

    public void publishAppointmentChangeAfterCommit() {
        afterCommit(() -> {
            publish(adminEmitters, "appointment-changed");
            publish(availabilityEmitters, "availability-changed");
        });
    }

    public void publishAvailabilityChangeAfterCommit() {
        afterCommit(() -> publish(availabilityEmitters, "availability-changed"));
    }

    @Scheduled(fixedRate = 20_000)
    public void sendHeartbeats() {
        heartbeat(adminEmitters);
        heartbeat(availabilityEmitters);
    }

    private SseEmitter subscribe(Set<SseEmitter> subscribers) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        subscribers.add(emitter);
        emitter.onCompletion(() -> subscribers.remove(emitter));
        emitter.onTimeout(() -> subscribers.remove(emitter));
        emitter.onError(error -> subscribers.remove(emitter));
        return emitter;
    }

    private void publish(Set<SseEmitter> subscribers, String eventName) {
        Map<String, String> eventData = Map.of("updatedAt", Instant.now().toString());
        subscribers.forEach(emitter -> send(emitter, SseEmitter.event().name(eventName).data(eventData)));
    }

    private void heartbeat(Set<SseEmitter> subscribers) {
        subscribers.forEach(emitter -> send(emitter, SseEmitter.event().comment("keep-alive")));
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException ex) {
            boolean removed = adminEmitters.remove(emitter);
            removed |= availabilityEmitters.remove(emitter);
            if (removed) log.debug("Removed a disconnected appointment event stream.");
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
