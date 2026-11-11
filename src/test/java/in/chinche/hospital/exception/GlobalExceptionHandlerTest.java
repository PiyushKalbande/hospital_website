package in.chinche.hospital.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void doesNotWriteJsonErrorEnvelopeToDisconnectedEventStream() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/availability/events");
        request.addHeader("Accept", "text/event-stream");

        var response = handler.unexpected(new IOException("Client disconnected"), request);

        assertEquals(204, response.getStatusCode().value());
        assertEquals(null, response.getBody());
    }
}