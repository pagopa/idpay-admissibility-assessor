package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.reactive.pdnd.exception.PdndServiceTooManyRequestException;
import it.gov.pagopa.common.reactive.pdnd.service.PdndRestClient;
import it.gov.pagopa.common.utils.TestUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;

import static it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest.WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX;
import static it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest.WIREMOCK_TEST_PROP2BASEPATH_SECURE_MAP_PREFIX;

@TestPropertySource(properties = {
                "logging.level.it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC001RestClientImpl=WARN",

        "app.anpr.config.https-config.enabled=true",
                // cert and key configured inside wiremockKeyStore.p12
                "app.anpr.config.https-config.cert=-----BEGIN CERTIFICATE-----\\nMIIDnzCCAoegAwIBAgIUJ8/0z+sR6Llr9FcIGoc5nvZQydgwDQYJKoZIhvcNAQEL\\nBQAwXzELMAkGA1UEBhMCSVQxDTALBgNVBAgMBFJPTUUxDTALBgNVBAcMBFJPTUUx\\nDjAMBgNVBAoMBUlEUEFZMQ4wDAYDVQQLDAVJRFBBWTESMBAGA1UEAwwJbG9jYWxo\\nb3N0MB4XDTIyMTEwOTE1MTI0NFoXDTMyMDkxNzE1MTI0NFowXzELMAkGA1UEBhMC\\nSVQxDTALBgNVBAgMBFJPTUUxDTALBgNVBAcMBFJPTUUxDjAMBgNVBAoMBUlEUEFZ\\nMQ4wDAYDVQQLDAVJRFBBWTESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG\\n9w0BAQEFAAOCAQ8AMIIBCgKCAQEArDOJKswwCaKdYJbaHZz3bgEIl7z1ArZpNI54\\nZGaXcRitiwjr/W9fenW69mG7IAlITuPtaIu4iggXTcSRuaulres2EvuP7KjL0tfo\\nx/PstqaMZzLF8wOYfJE4iJ8ffcQL67LJ3/Wwn2FhYVV+4D2AYW8QPdRm406HJG7b\\nNKLmdM9AFUQp6zoTvNegyWQyAfH40i72UopltDubcAykD6YgkRctCtKd8h/BRpIR\\ntMn0AGLM/o5qwYu+eCAy8/7Ppj3HzCwHkDOJad/g2pRj4soJdvn5rP6TM4OVtZ7V\\nehxionkaccBPcyDGSrIo5837XYaGv3r7Rn0rCplfxnU4Gtmd5wIDAQABo1MwUTAd\\nBgNVHQ4EFgQUPYfJeHRHwSLmcueB8jUQSHUReVIwHwYDVR0jBBgwFoAUPYfJeHRH\\nwSLmcueB8jUQSHUReVIwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOC\\nAQEAK34LEHSVM44Wwbs9nKDKeQTRGosdd+gQSrqGf3nI0vkhckuaoYPnuFKi+eo2\\nr+J6xXgqhQfrvhXnYxNEJr9U+9ELBc3IjG6bTUS6HyWhu2PJCeckxQJqonVntl99\\njmEr4G7QJeDc9oJmC0NJqBmQS/D0tMxChNWpYe1AoGXwqc4S6NTd3x2Z8THzv8du\\nMMn7+1f/VOWe7/Iuuvx5DHN2JFi0lvhMqwglIweGn/qLGB0+r9GM+QlfGuZvUey2\\nx3C0DLQnNIkNKktGjaNjCmpZcd9SIVi6TOPpR+AxlIddYvUXu4GYVXyfDPgzPeha\\nJDiI4WMkIMmYSzhMc/lfuDMGow==\\n-----END CERTIFICATE-----",
                "app.anpr.config.https-config.key=-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCsM4kqzDAJop1g\\nltodnPduAQiXvPUCtmk0jnhkZpdxGK2LCOv9b196dbr2YbsgCUhO4+1oi7iKCBdN\\nxJG5q6Wt6zYS+4/sqMvS1+jH8+y2poxnMsXzA5h8kTiInx99xAvrssnf9bCfYWFh\\nVX7gPYBhbxA91GbjTockbts0ouZ0z0AVRCnrOhO816DJZDIB8fjSLvZSimW0O5tw\\nDKQPpiCRFy0K0p3yH8FGkhG0yfQAYsz+jmrBi754IDLz/s+mPcfMLAeQM4lp3+Da\\nlGPiygl2+fms/pMzg5W1ntV6HGKieRpxwE9zIMZKsijnzftdhoa/evtGfSsKmV/G\\ndTga2Z3nAgMBAAECggEAEC6FmMJ4Tyd7T3zNgVPjQnCRbKTihz858qjislibqZKO\\nmE6d0oJ5P+o5R/bWHUQSCevMPvNGQ55QBkxO/1ocZxP/0FfYZf5UrPsCEmwfFejf\\nr8DrLhNr7GS/IcOGM4zNK/hwlP2i+88sVfexRQQygLVtmsnPY1PZSjiqm68lJdu+\\naP8TYM10y1aeiYnfuUYvnvXJFXeTEockhaUJTmeIQNbbUy+pyJ0mAPASPtXRLr8h\\nUflutICnWcx4v/qkCn1jmHw+NMA4q7hOH7UuOAqj53FqGMN+IWfjMmmYoQ7MVURx\\n8CrnEtlCOua+C8EEIFL2ylvV7X0cv/DqCJLVQoegsQKBgQDLzMaAjNgD8xSXp+Gj\\nbeeUsSGptEaGMuA89AzyTnCyvU9a1HGwDAghoQPae+pVk7R5uokojWkBVzP/kKxv\\nZldGwPOegUUdBLS4yJML+OkqtoCgf3Mbcozm5dVYtx7bYdhh3PswzRmn/h/YjEAz\\n+/mxi6dJir0k0Nd4YNtQbzBctwKBgQDYTtSmJvVQdOHnzqA/LRmMF1l+HaqLuDfu\\nB4rDlxCdDfOAvHqz+3YapP3B4MQuz29TSDqwAnzoN2XZX5B6g/jKauWpAwZkFXuO\\nfqcfNG/+MewTcHIYNm+EtgXtIsnCXDfAeXdQapzNsOX+XSF/aWcgGHg18xOBPt0R\\n7Aoa/h34UQKBgQCsCzGjwcJ2CxXeNPYxfg1ao/HUDoDet0I/kpL/VqKi8Vd1SRS0\\nVmPi58eWALfBCJD5ljRFjKMRY6lc3KgE3vNconTG4UAUEC30NDaWi8liqnCJjS4C\\nBMDYBzwEyYn+D2qYqvFOsEYxYEFIEJX+jH+sl0VguwOTec38LF/YVhUQnwKBgG5u\\n2Kw3SZkZA1ioqjF24gsexKbZmH+avps8qICw+F9mhwIbt/15jVOPFqrMCPzpFKoN\\nP0ErFAAugEYZPxb9l6AoMTY3gCTKvvkB+mq5B9BcRm2qQ+XOrOKxV5c44o7jK+eN\\nW/fnZkSxYsqZW4fEFU1SkNTiU/vxT0ZeHs6nHD/xAoGAOIqaqQnJfGj/wLo3Z9o5\\n/Oxu1zTPGZC6SqpdygCjlQ0kQ8Bp0LV7nL06/VCHAHI2lF12xApRnFk7GY3xyqK8\\nnYxeRASCj3GGmLupGshtfCtDBeysE2h7kj3Bo0d6g1Ye+j8BUZuZaZm6WNlo7cgE\\nNLHn1k0IpmXFOiFa1Y1D6Bc=\\n-----END PRIVATE KEY-----",

                "app.anpr.config.https-config.mutualAuthEnabled=true",
                // Wiremock configured to use same keystore: wiremockKeyStore.p12
        "app.anpr.config.https-config.trustCertificatesCollection=-----BEGIN CERTIFICATE-----\\nMIIDnzCCAoegAwIBAgIUJ8/0z+sR6Llr9FcIGoc5nvZQydgwDQYJKoZIhvcNAQEL\\nBQAwXzELMAkGA1UEBhMCSVQxDTALBgNVBAgMBFJPTUUxDTALBgNVBAcMBFJPTUUx\\nDjAMBgNVBAoMBUlEUEFZMQ4wDAYDVQQLDAVJRFBBWTESMBAGA1UEAwwJbG9jYWxo\\nb3N0MB4XDTIyMTEwOTE1MTI0NFoXDTMyMDkxNzE1MTI0NFowXzELMAkGA1UEBhMC\\nSVQxDTALBgNVBAgMBFJPTUUxDTALBgNVBAcMBFJPTUUxDjAMBgNVBAoMBUlEUEFZ\\nMQ4wDAYDVQQLDAVJRFBBWTESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG\\n9w0BAQEFAAOCAQ8AMIIBCgKCAQEArDOJKswwCaKdYJbaHZz3bgEIl7z1ArZpNI54\\nZGaXcRitiwjr/W9fenW69mG7IAlITuPtaIu4iggXTcSRuaulres2EvuP7KjL0tfo\\nx/PstqaMZzLF8wOYfJE4iJ8ffcQL67LJ3/Wwn2FhYVV+4D2AYW8QPdRm406HJG7b\\nNKLmdM9AFUQp6zoTvNegyWQyAfH40i72UopltDubcAykD6YgkRctCtKd8h/BRpIR\\ntMn0AGLM/o5qwYu+eCAy8/7Ppj3HzCwHkDOJad/g2pRj4soJdvn5rP6TM4OVtZ7V\\nehxionkaccBPcyDGSrIo5837XYaGv3r7Rn0rCplfxnU4Gtmd5wIDAQABo1MwUTAd\\nBgNVHQ4EFgQUPYfJeHRHwSLmcueB8jUQSHUReVIwHwYDVR0jBBgwFoAUPYfJeHRH\\nwSLmcueB8jUQSHUReVIwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOC\\nAQEAK34LEHSVM44Wwbs9nKDKeQTRGosdd+gQSrqGf3nI0vkhckuaoYPnuFKi+eo2\\nr+J6xXgqhQfrvhXnYxNEJr9U+9ELBc3IjG6bTUS6HyWhu2PJCeckxQJqonVntl99\\njmEr4G7QJeDc9oJmC0NJqBmQS/D0tMxChNWpYe1AoGXwqc4S6NTd3x2Z8THzv8du\\nMMn7+1f/VOWe7/Iuuvx5DHN2JFi0lvhMqwglIweGn/qLGB0+r9GM+QlfGuZvUey2\\nx3C0DLQnNIkNKktGjaNjCmpZcd9SIVi6TOPpR+AxlIddYvUXu4GYVXyfDPgzPeha\\nJDiI4WMkIMmYSzhMc/lfuDMGow==\\n-----END CERTIFICATE-----",
        WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX + "app.pdnd.base-url=pdnd",
        WIREMOCK_TEST_PROP2BASEPATH_SECURE_MAP_PREFIX + "app.anpr.config.base-url=anpr/"
})
public class AnprC001RestClientImplIntegrationTest extends BaseIntegrationTest {

    public static final String FISCAL_CODE_OK = "CF_OK";
    public static final String FISCAL_CODE_NOTFOUND = "CF_NOT_FOUND";
    public static final String FISCAL_CODE_INVALIDREQUEST = "CF_INVALID_REQUEST";
    public static final String FISCAL_CODE_TOOMANYREQUESTS = "CF_ANPR_TOO_MANY_REQUESTS";

    public static final PdndInitiativeConfig PDND_INITIATIVE_CONFIG = new PdndInitiativeConfig(
            "CLIENTID",
            "KID",
            "PURPOSEID"
    );

    @SpyBean
    private ObjectMapper objectMapper;
    @SpyBean
    private PdndRestClient pdndRestClient;

    @Autowired
    private AnprC001RestClient anprC001RestClient;

    @BeforeEach
    void clearCache() throws IllegalAccessException {
        Field cacheField = ReflectionUtils.findField(anprC001RestClient.getClass(), "pdndAuthDataCache");
        Assertions.assertNotNull(cacheField);
        cacheField.setAccessible(true);
        Cache<?,?> cache = (Cache<?,?>) cacheField.get(anprC001RestClient);
        cache.invalidateAll();
    }

    @Test
    void getResidenceAssessment(){
        // When
        RispostaE002OKDTO result = anprC001RestClient.invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG).block();

        // Then
        RispostaE002OKDTO expectedResponse = buildExpectedResponse();
        expectedResponse.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita().getCodiceFiscale().setCodFiscale(FISCAL_CODE_OK);
        Assertions.assertEquals(expectedResponse, result);

        // accessToken cached for each pdndInitiativeConfig
        RispostaE002OKDTO result2ndInvoke = anprC001RestClient.invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG).block();
        Assertions.assertEquals(expectedResponse, result2ndInvoke);

        Mockito.verify(pdndRestClient).createToken(Mockito.eq(PDND_INITIATIVE_CONFIG.getClientId()), Mockito.anyString());

        // new clientId
        PdndInitiativeConfig pdndInitiativeConfig2 = new PdndInitiativeConfig(
                "CLIENTID2",
                "KID",
                "PURPOSEID"
        );
        RispostaE002OKDTO resultNewClientId = anprC001RestClient.invoke(FISCAL_CODE_OK, pdndInitiativeConfig2).block();
        Assertions.assertEquals(expectedResponse, resultNewClientId);

        Mockito.verify(pdndRestClient, Mockito.times(2)).createToken(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testNotFound(){
        //When
        RispostaE002OKDTO result = anprC001RestClient.invoke(FISCAL_CODE_NOTFOUND, PDND_INITIATIVE_CONFIG).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getListaSoggetti());
    }

    @Test
    void testInvalidRequest(){
        Mono<RispostaE002OKDTO> mono = anprC001RestClient.invoke(FISCAL_CODE_INVALIDREQUEST, PDND_INITIATIVE_CONFIG);
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, mono::block);
        Assertions.assertTrue(exception.getMessage().startsWith("[PDND_SERVICE_INVOKE] Something went wrong when invoking PDND service https://modipa-val.anpr.interno.it/govway/rest/in/MinInternoPortaANPR/C001-servizioNotifica/v1: 400 Bad Request"));
    }

    @Test
    void testTooManyRequests(){
        Mono<RispostaE002OKDTO> mono = anprC001RestClient.invoke(FISCAL_CODE_TOOMANYREQUESTS, PDND_INITIATIVE_CONFIG);
        Assertions.assertThrows(PdndServiceTooManyRequestException.class, mono::block);
    }

    @Test
    @SneakyThrows
    void objectMapperException(){
        // Given
        Mockito.when(objectMapper.writeValueAsString(Mockito.any())).thenThrow(JsonProcessingException.class);

        // When
        try {
            anprC001RestClient.invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG).block();
        }catch (Exception e){
            // Then
            Assertions.assertTrue(e instanceof IllegalStateException);
        }
    }

    public static RispostaE002OKDTO buildExpectedResponse() {
        try {
            return TestUtils.objectMapper.readValue("""
                    {
                    	"listaSoggetti": {
                    		"datiSoggetto": [
                    			{
                    				"generalita": {
                    					"codiceFiscale": {
                    						"codFiscale": "STTSGT90A01H501J",
                    						"validitaCF": "9"
                    					},
                    					"cognome": "SETTIMO",
                    					"dataNascita": "1990-01-01",
                    					"idSchedaSoggettoANPR": "2775118",
                    					"luogoNascita": {
                    						"comune": {
                    							"codiceIstat": "058091",
                    							"nomeComune": "ROMA",
                    							"siglaProvinciaIstat": "RM"
                    						}
                    					},
                    					"nome": "SOGGETTO",
                    					"sesso": "M"
                    				},
                    				"identificativi": {
                    					"idANPR": "AF41450AS"
                    				},
                    				"infoSoggettoEnte": [
                    					{
                    						"chiave": "Verifica esistenza in vita",
                    						"id": "1003",
                    						"valore": "S"
                    					}
                    				],
                    				"residenza": [
                    					{
                    						"indirizzo": {
                    							"cap": "41026",
                    							"comune": {
                    								"codiceIstat": "036030",
                    								"nomeComune": "PAVULLO NEL FRIGNANO",
                    								"siglaProvinciaIstat": "MO"
                    							},
                    							"numeroCivico": {
                    								"civicoInterno": {
                    									"interno1": "3",
                    									"scala": "B4"
                    								},
                    								"numero": "55"
                    							},
                    							"toponimo": {
                    								"denominazioneToponimo": "AMERIGO VESPUCCI",
                    								"specie": "VIA",
                    								"specieFonte": "1"
                    							}
                    						},
                    						"tipoIndirizzo": "1"
                    					}
                    				]
                    			}
                    		]
                    	},
                    	"idOperazioneANPR": "58370927"
                    }
                    """, RispostaE002OKDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read expected response", e);
        }
    }

}