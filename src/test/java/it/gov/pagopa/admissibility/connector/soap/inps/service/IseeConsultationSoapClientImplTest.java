package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.config.InpsConfiguration;
import it.gov.pagopa.admissibility.connector.soap.inps.config.InpsClientConfig;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.EsitoEnum;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.TypeEsitoConsultazioneIndicatore;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest;
import it.gov.pagopa.common.soap.service.SoapLoggingHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest.WIREMOCK_TEST_PROP2BASEPATH_SECURE_MAP_PREFIX;

@ContextConfiguration(
        classes = {
                IseeConsultationSoapClientImpl.class,
                InpsClientConfig.class,
                InpsConfiguration.class,
                SoapLoggingHandler.class,
                WebClientConfig.class
        })
@EnableConfigurationProperties(value = InpsConfiguration.class)
@TestPropertySource(
        properties = {
                WIREMOCK_TEST_PROP2BASEPATH_SECURE_MAP_PREFIX + "app.inps.iseeConsultation.base-url=inps/isee",

                "app.inps-mock.enabled-isee=false",
                "app.inps-mock.base-url=http://idpay-mock-baseurl",
                "app.inps.iseeConsultation.base-url=inps/isee",
                "app.inps.iseeConsultation.config.connection-timeout=10000",
                "app.inps.iseeConsultation.config.request-timeout=60000",
                "app.inps.header.userId=OperationBatchIDPay",
                "app.inps.header.officeCode=001",
//                "app.inps.secure.cert=-----BEGIN CERTIFICATE-----\\nMIIDnzCCAoegAwIBAgIUJ8/0z+sR6Llr9FcIGoc5nvZQydgwDQYJKoZIhvcNAQEL\\nBQAwXzELMAkGA1UEBhMCSVQxDTALBgNVBAgMBFJPTUUxDTALBgNVBAcMBFJPTUUx\\nDjAMBgNVBAoMBUlEUEFZMQ4wDAYDVQQLDAVJRFBBWTESMBAGA1UEAwwJbG9jYWxo\\nb3N0MB4XDTIyMTEwOTE1MTI0NFoXDTMyMDkxNzE1MTI0NFowXzELMAkGA1UEBhMC\\nSVQxDTALBgNVBAgMBFJPTUUxDTALBgNVBAcMBFJPTUUxDjAMBgNVBAoMBUlEUEFZ\\nMQ4wDAYDVQQLDAVJRFBBWTESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG\\n9w0BAQEFAAOCAQ8AMIIBCgKCAQEArDOJKswwCaKdYJbaHZz3bgEIl7z1ArZpNI54\\nZGaXcRitiwjr/W9fenW69mG7IAlITuPtaIu4iggXTcSRuaulres2EvuP7KjL0tfo\\nx/PstqaMZzLF8wOYfJE4iJ8ffcQL67LJ3/Wwn2FhYVV+4D2AYW8QPdRm406HJG7b\\nNKLmdM9AFUQp6zoTvNegyWQyAfH40i72UopltDubcAykD6YgkRctCtKd8h/BRpIR\\ntMn0AGLM/o5qwYu+eCAy8/7Ppj3HzCwHkDOJad/g2pRj4soJdvn5rP6TM4OVtZ7V\\nehxionkaccBPcyDGSrIo5837XYaGv3r7Rn0rCplfxnU4Gtmd5wIDAQABo1MwUTAd\\nBgNVHQ4EFgQUPYfJeHRHwSLmcueB8jUQSHUReVIwHwYDVR0jBBgwFoAUPYfJeHRH\\nwSLmcueB8jUQSHUReVIwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOC\\nAQEAK34LEHSVM44Wwbs9nKDKeQTRGosdd+gQSrqGf3nI0vkhckuaoYPnuFKi+eo2\\nr+J6xXgqhQfrvhXnYxNEJr9U+9ELBc3IjG6bTUS6HyWhu2PJCeckxQJqonVntl99\\njmEr4G7QJeDc9oJmC0NJqBmQS/D0tMxChNWpYe1AoGXwqc4S6NTd3x2Z8THzv8du\\nMMn7+1f/VOWe7/Iuuvx5DHN2JFi0lvhMqwglIweGn/qLGB0+r9GM+QlfGuZvUey2\\nx3C0DLQnNIkNKktGjaNjCmpZcd9SIVi6TOPpR+AxlIddYvUXu4GYVXyfDPgzPeha\\nJDiI4WMkIMmYSzhMc/lfuDMGow==\\n-----END CERTIFICATE-----",
                "app.inps.secure.cert=LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURuekNDQW9lZ0F3SUJBZ0lVSjgvMHorc1I2TGxyOUZjSUdvYzVudlpReWRnd0RRWUpLb1pJaHZjTkFRRUwKQlFBd1h6RUxNQWtHQTFVRUJoTUNTVlF4RFRBTEJnTlZCQWdNQkZKUFRVVXhEVEFMQmdOVkJBY01CRkpQVFVVeApEakFNQmdOVkJBb01CVWxFVUVGWk1RNHdEQVlEVlFRTERBVkpSRkJCV1RFU01CQUdBMVVFQXd3SmJHOWpZV3hvCmIzTjBNQjRYRFRJeU1URXdPVEUxTVRJME5Gb1hEVE15TURreE56RTFNVEkwTkZvd1h6RUxNQWtHQTFVRUJoTUMKU1ZReERUQUxCZ05WQkFnTUJGSlBUVVV4RFRBTEJnTlZCQWNNQkZKUFRVVXhEakFNQmdOVkJBb01CVWxFVUVGWgpNUTR3REFZRFZRUUxEQVZKUkZCQldURVNNQkFHQTFVRUF3d0piRzlqWVd4b2IzTjBNSUlCSWpBTkJna3Foa2lHCjl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUFyRE9KS3N3d0NhS2RZSmJhSFp6M2JnRUlsN3oxQXJacE5JNTQKWkdhWGNSaXRpd2pyL1c5ZmVuVzY5bUc3SUFsSVR1UHRhSXU0aWdnWFRjU1J1YXVscmVzMkV2dVA3S2pMMHRmbwp4L1BzdHFhTVp6TEY4d09ZZkpFNGlKOGZmY1FMNjdMSjMvV3duMkZoWVZWKzREMkFZVzhRUGRSbTQwNkhKRzdiCk5LTG1kTTlBRlVRcDZ6b1R2TmVneVdReUFmSDQwaTcyVW9wbHREdWJjQXlrRDZZZ2tSY3RDdEtkOGgvQlJwSVIKdE1uMEFHTE0vbzVxd1l1K2VDQXk4LzdQcGozSHpDd0hrRE9KYWQvZzJwUmo0c29KZHZuNXJQNlRNNE9WdFo3VgplaHhpb25rYWNjQlBjeURHU3JJbzU4MzdYWWFHdjNyN1JuMHJDcGxmeG5VNEd0bWQ1d0lEQVFBQm8xTXdVVEFkCkJnTlZIUTRFRmdRVVBZZkplSFJId1NMbWN1ZUI4alVRU0hVUmVWSXdId1lEVlIwakJCZ3dGb0FVUFlmSmVIUkgKd1NMbWN1ZUI4alVRU0hVUmVWSXdEd1lEVlIwVEFRSC9CQVV3QXdFQi96QU5CZ2txaGtpRzl3MEJBUXNGQUFPQwpBUUVBSzM0TEVIU1ZNNDRXd2JzOW5LREtlUVRSR29zZGQrZ1FTcnFHZjNuSTB2a2hja3Vhb1lQbnVGS2krZW8yCnIrSjZ4WGdxaFFmcnZoWG5ZeE5FSnI5VSs5RUxCYzNJakc2YlRVUzZIeVdodTJQSkNlY2t4UUpxb25WbnRsOTkKam1FcjRHN1FKZURjOW9KbUMwTkpxQm1RUy9EMHRNeENoTldwWWUxQW9HWHdxYzRTNk5UZDN4Mlo4VEh6djhkdQpNTW43KzFmL1ZPV2U3L0l1dXZ4NURITjJKRmkwbHZoTXF3Z2xJd2VHbi9xTEdCMCtyOUdNK1FsZkd1WnZVZXkyCngzQzBETFFuTklrTktrdEdqYU5qQ21wWmNkOVNJVmk2VE9QcFIrQXhsSWRkWXZVWHU0R1lWWHlmRFBnelBlaGEKSkRpSTRXTWtJTW1ZU3poTWMvbGZ1RE1Hb3c9PQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0t",
                "app.inps.secure.key=-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCsM4kqzDAJop1g\\nltodnPduAQiXvPUCtmk0jnhkZpdxGK2LCOv9b196dbr2YbsgCUhO4+1oi7iKCBdN\\nxJG5q6Wt6zYS+4/sqMvS1+jH8+y2poxnMsXzA5h8kTiInx99xAvrssnf9bCfYWFh\\nVX7gPYBhbxA91GbjTockbts0ouZ0z0AVRCnrOhO816DJZDIB8fjSLvZSimW0O5tw\\nDKQPpiCRFy0K0p3yH8FGkhG0yfQAYsz+jmrBi754IDLz/s+mPcfMLAeQM4lp3+Da\\nlGPiygl2+fms/pMzg5W1ntV6HGKieRpxwE9zIMZKsijnzftdhoa/evtGfSsKmV/G\\ndTga2Z3nAgMBAAECggEAEC6FmMJ4Tyd7T3zNgVPjQnCRbKTihz858qjislibqZKO\\nmE6d0oJ5P+o5R/bWHUQSCevMPvNGQ55QBkxO/1ocZxP/0FfYZf5UrPsCEmwfFejf\\nr8DrLhNr7GS/IcOGM4zNK/hwlP2i+88sVfexRQQygLVtmsnPY1PZSjiqm68lJdu+\\naP8TYM10y1aeiYnfuUYvnvXJFXeTEockhaUJTmeIQNbbUy+pyJ0mAPASPtXRLr8h\\nUflutICnWcx4v/qkCn1jmHw+NMA4q7hOH7UuOAqj53FqGMN+IWfjMmmYoQ7MVURx\\n8CrnEtlCOua+C8EEIFL2ylvV7X0cv/DqCJLVQoegsQKBgQDLzMaAjNgD8xSXp+Gj\\nbeeUsSGptEaGMuA89AzyTnCyvU9a1HGwDAghoQPae+pVk7R5uokojWkBVzP/kKxv\\nZldGwPOegUUdBLS4yJML+OkqtoCgf3Mbcozm5dVYtx7bYdhh3PswzRmn/h/YjEAz\\n+/mxi6dJir0k0Nd4YNtQbzBctwKBgQDYTtSmJvVQdOHnzqA/LRmMF1l+HaqLuDfu\\nB4rDlxCdDfOAvHqz+3YapP3B4MQuz29TSDqwAnzoN2XZX5B6g/jKauWpAwZkFXuO\\nfqcfNG/+MewTcHIYNm+EtgXtIsnCXDfAeXdQapzNsOX+XSF/aWcgGHg18xOBPt0R\\n7Aoa/h34UQKBgQCsCzGjwcJ2CxXeNPYxfg1ao/HUDoDet0I/kpL/VqKi8Vd1SRS0\\nVmPi58eWALfBCJD5ljRFjKMRY6lc3KgE3vNconTG4UAUEC30NDaWi8liqnCJjS4C\\nBMDYBzwEyYn+D2qYqvFOsEYxYEFIEJX+jH+sl0VguwOTec38LF/YVhUQnwKBgG5u\\n2Kw3SZkZA1ioqjF24gsexKbZmH+avps8qICw+F9mhwIbt/15jVOPFqrMCPzpFKoN\\nP0ErFAAugEYZPxb9l6AoMTY3gCTKvvkB+mq5B9BcRm2qQ+XOrOKxV5c44o7jK+eN\\nW/fnZkSxYsqZW4fEFU1SkNTiU/vxT0ZeHs6nHD/xAoGAOIqaqQnJfGj/wLo3Z9o5\\n/Oxu1zTPGZC6SqpdygCjlQ0kQ8Bp0LV7nL06/VCHAHI2lF12xApRnFk7GY3xyqK8\\nnYxeRASCj3GGmLupGshtfCtDBeysE2h7kj3Bo0d6g1Ye+j8BUZuZaZm6WNlo7cgE\\nNLHn1k0IpmXFOiFa1Y1D6Bc=\\n-----END PRIVATE KEY-----"
        }
)
public class IseeConsultationSoapClientImplTest extends BaseWireMockTest {

    public static final String FISCAL_CODE_OK = "CF_OK";
    public static final String FISCAL_CODE_NOTFOUND = "CF_NOT_FOUND";
    public static final String FISCAL_CODE_INVALIDREQUEST = "CF_INVALID_REQUEST";
    public static final String FISCAL_CODE_RETRY = "CF_INPS_RETRY";
    public static final String FISCAL_CODE_UNEXPECTED_RESULT_CODE = "CF_INPS_UNEXPECTED_RESULT_CODE";
    public static final String FISCAL_CODE_FAULT_MESSAGE = "CF_INPS_FAULT_MESSAGE";
    public static final String FISCAL_CODE_TOOMANYREQUESTS = "CF_INPS_TOO_MANY_REQUESTS";



    @Autowired
    private IseeConsultationSoapClientImpl iseeConsultationSoapClient;

    @Test
    void callService() {
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee(FISCAL_CODE_OK, IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(EsitoEnum.OK, result.getEsito());
        Assertions.assertEquals(0, result.getIdRichiesta());

        TypeEsitoConsultazioneIndicatore indicatore = InpsDataRetrieverServiceImpl.readResultFromXmlString(new String(result.getXmlEsitoIndicatore(), StandardCharsets.UTF_8));
        Assertions.assertNotNull(indicatore);
        Assertions.assertEquals(BigDecimal.valueOf(10_000), indicatore.getISEE());
    }

    @Test
    void getIseeNotFound() {
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee(FISCAL_CODE_NOTFOUND, IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(EsitoEnum.DATI_NON_TROVATI, result.getEsito());
    }

    @Test
    void getIseeInvalidRequest() {
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee(FISCAL_CODE_INVALIDREQUEST, IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(EsitoEnum.RICHIESTA_INVALIDA, result.getEsito());
    }

    @Test
    void getIseeRetry() {
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee(FISCAL_CODE_RETRY, IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNull(result);
    }

    @Test
    void getIseeUnexpectedResultCode() {
        Mono<ConsultazioneIndicatoreResponseType> mono = iseeConsultationSoapClient.getIsee(FISCAL_CODE_UNEXPECTED_RESULT_CODE, IseeTypologyEnum.ORDINARIO);
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, mono::block);
        Assertions.assertEquals("[ONBOARDING_REQUEST][INPS_INVOCATION] Something went wrong when invoking INPS service", exception.getMessage());
    }

    @Test
    void getIseeTooManyRequests() {
        Mono<ConsultazioneIndicatoreResponseType> mono = iseeConsultationSoapClient.getIsee(FISCAL_CODE_TOOMANYREQUESTS, IseeTypologyEnum.ORDINARIO);
        Assertions.assertThrows(InpsDailyRequestLimitException.class, mono::block);
    }

    @Test
    void getIseeFaultMessage() {
        Mono<ConsultazioneIndicatoreResponseType> mono = iseeConsultationSoapClient.getIsee(FISCAL_CODE_FAULT_MESSAGE, IseeTypologyEnum.ORDINARIO);
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, mono::block);
        Assertions.assertEquals("[ONBOARDING_REQUEST][INPS_INVOCATION] Something went wrong when invoking INPS service", exception.getMessage());

    }
}
