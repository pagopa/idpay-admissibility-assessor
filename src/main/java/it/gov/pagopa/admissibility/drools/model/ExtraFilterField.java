package it.gov.pagopa.admissibility.drools.model;

import lombok.Data;

import java.util.List;

@Data
public class ExtraFilterField {
    private String path;
    private String name;
    private String field;
    private Class<?> type;
    private Class<?> castPath;
    private boolean toCast;
    private List<Class<?>> subclasses;
}
