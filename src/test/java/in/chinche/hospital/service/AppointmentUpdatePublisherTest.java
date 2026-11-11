package in.chinche.hospital.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AppointmentUpdatePublisherTest {

    @Test
    void removesFailedHeartbeatEmitterWithoutCompletingAnAlreadyFailedAsyncContext() throws Exception {
        AppointmentUpdatePublisher publisher = new AppointmentUpdatePublisher();
        @SuppressWarnings("unchecked")
        Set<SseEmitter> subscribers = (Set<SseEmitter>) ReflectionTestUtils.getField(publisher, "adminEmitters");
        SseEmitter emitter = mock(SseEmitter.class);
        subscribers.add(emitter);
        doThrow(new IllegalStateException("AsyncContext already failed"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        publisher.sendHeartbeats();

        assertFalse(subscribers.contains(emitter));
        verify(emitter, never()).completeWithError(any(Throwable.class));
    }
}