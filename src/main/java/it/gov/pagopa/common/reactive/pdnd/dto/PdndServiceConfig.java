package it.gov.pagopa.common.reactive.pdnd.dto;

import it.gov.pagopa.common.reactive.pdnd.config.BasePdndServiceProviderConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Predicate;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PdndServiceConfig<R> extends BasePdndServiceProviderConfig {

    private static final Predicate<Throwable> DEFAULT_TOOMANYREQUEST_PREDICATE = e -> e instanceof WebClientResponseException.TooManyRequests;

// region BasePdndServiceConfig properties
    private String audience;
    private HttpMethod httpMethod;
    private String path;
//endregion

    private Class<R> responseBodyClass;
    private Predicate<? super Throwable> tooManyRequestPredicate = DEFAULT_TOOMANYREQUEST_PREDICATE;
    @Getter
    private R emptyResponseBody;


    public void setResponseBodyClass(Class<R> responseBodyClass) {
        this.responseBodyClass = responseBodyClass;
        try {
            emptyResponseBody = responseBodyClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Cannot create instance of " + responseBodyClass + " using no args constructor", e);
        }
    }
}
