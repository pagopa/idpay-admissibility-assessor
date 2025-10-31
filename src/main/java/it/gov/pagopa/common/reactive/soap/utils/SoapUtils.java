package it.gov.pagopa.common.reactive.soap.utils;

import jakarta.xml.ws.AsyncHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
public class SoapUtils {
    private SoapUtils() {}

    /** To build an AsyncHandler used to convert Soap response into a reactive event */
    private static <T> AsyncHandler<T> into(MonoSink<T> sink) {
        return res -> {
            try {
                sink.success(res.get(1, TimeUnit.MILLISECONDS));
            } catch (ExecutionException | TimeoutException e) {
                log.error("""
                ERROR MESSAGE : {}
                ERROR CAUSE : {}
                """, e.getMessage(), e.getCause());
                sink.error(e);
            } catch (InterruptedException e) {
                log.warn("Interrupted!", e);
                Thread.currentThread().interrupt();
            }
        };
    }

    /** To convert an async soap invocation into a Mono */
    public static <T> Mono<T> soapInvoke2Mono(Consumer<AsyncHandler<T>> invoke){
        return Mono.create(
                sink -> invoke.accept(into(sink)));
    }
}