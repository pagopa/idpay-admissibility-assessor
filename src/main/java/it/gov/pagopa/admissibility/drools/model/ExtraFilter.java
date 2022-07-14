package it.gov.pagopa.admissibility.drools.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.gov.pagopa.admissibility.drools.model.aggregator.Aggregator;
import it.gov.pagopa.admissibility.drools.model.aggregator.AggregatorAnd;
import it.gov.pagopa.admissibility.drools.model.aggregator.AggregatorOr;
import it.gov.pagopa.admissibility.drools.model.filter.Filter;

import java.io.Serializable;

/** Extra filter to apply.
 * @see Filter
 * @see Aggregator
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Filter.class, name = "filter"),
        @JsonSubTypes.Type(value = AggregatorAnd.class, name = "aggregator-and"),
        @JsonSubTypes.Type(value = AggregatorOr.class, name = "aggregator-or"),
        @JsonSubTypes.Type(value = NotOperation.class, name = "not") })
public interface ExtraFilter extends Serializable {
}
