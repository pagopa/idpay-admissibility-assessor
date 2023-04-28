package it.gov.pagopa.admissibility.connector.soap.inps;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.MonoSink;

import javax.xml.ws.AsyncHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
class ReactorAsyncHandler {

    private ReactorAsyncHandler() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> AsyncHandler<T> into(MonoSink<T> sink) {
        return res -> {
            try {
                sink.success(res.get(1, TimeUnit.MILLISECONDS));
            } catch (ExecutionException | TimeoutException e) {
                sink.error(e);
            } catch (InterruptedException e) {
                log.warn("Interrupted!", e);
                Thread.currentThread().interrupt();
            }
        };
    }
}