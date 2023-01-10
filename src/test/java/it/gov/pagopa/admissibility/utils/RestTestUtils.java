package it.gov.pagopa.admissibility.utils;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class RestTestUtils {
    public static WireMockConfiguration getWireMockConfiguration(){
        return wireMockConfig()
                .dynamicPort()
                .usingFilesUnderClasspath("src/test/resources/stub");
    }
}
