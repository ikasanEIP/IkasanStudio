package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Filters Flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class FiltersFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.filtersflow.FiltersFlow",
                "getFiltersFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // connect to previous flow
        assertFactory("org.ikasan.studio.boot.flow.filtersflow.ComponentFactoryFiltersFlow",
                "getConnectToPreviousFlow", "org.ikasan.spec.component.endpoint.Consumer");
        // Default filter every 5th
        assertImplementation("org.example.cat4.filtersflow.DefaultFilterEvery5th", "org.ikasan.spec.component.filter.FilterRule");
        // filter every 4th
        assertImplementation("org.example.cat4.filtersflow.FilterEvery4th", "org.ikasan.spec.component.filter.Filter");
        // connect to next flow
        assertFactory("org.ikasan.studio.boot.flow.filtersflow.ComponentFactoryFiltersFlow",
                "getConnectToNextFlow", "org.ikasan.spec.component.endpoint.Producer");
    }

}
