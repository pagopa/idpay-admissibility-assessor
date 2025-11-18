package it.gov.pagopa.admissibility.connector.soap.inps.service;

import com.sun.xml.ws.client.ClientTransportException;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsGenericException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
public final class IseeUtils {

    private IseeUtils(){}

    public static <T, R> Mono<R> handleServiceOutcome(
            R result,
            T esito,
            int id,
            String descrizioneErrore,
            Set<T> retraybleOutcomes,
            T statusOk
    ) {
        if (retraybleOutcomes.contains(esito)) {
            log.warn("[INPS_INVOCATION] Retryable outcome! id={} esito={} err={}",
                    id, esito, descrizioneErrore);
            return Mono.empty();
        } else {
            if (!statusOk.equals(esito)) {
                log.error("[INPS_INVOCATION] No data! esito={} id={} err={}",
                        esito, id, descrizioneErrore);
            }
            return Mono.just(result);
        }
    }

    public static <T> Mono<T> handleError(Throwable e) {
        if (e instanceof ExecutionException
                && e.getCause() instanceof ClientTransportException clientTransportException
                && clientTransportException.getMessage().contains("Too Many Requests")) {
            return Mono.error(new InpsDailyRequestLimitException(e));
        } else {
            return Mono.error(new InpsGenericException(
                    "[ONBOARDING_REQUEST][INPS_INVOCATION] Something went wrong when invoking INPS service", e));
        }
    }

}
