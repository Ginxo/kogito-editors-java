package org.jboss.errai.ioc.tests.rebind;

import org.jboss.errai.ioc.rebind.ioc.codegen.builder.StatementBuilder;

/**
 * Base class for all {@link StatementBuilder} tests.
 *
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class AbstractStatementBuilderTest {

    /**
     * assertEquals, less sensitive to indentation differences.
     * <p/>
     * TODO compare parsed ASTs instead?
     *
     * @param expected
     * @param actual
     */
    protected static void assertEquals(String expected, String actual) {
        org.junit.Assert.assertEquals(expected.replaceAll("\\s+", " "),
                actual.replaceAll("\\s+", " "));
    }

    protected static void assertEquals(String message, String expected, String actual) {
        org.junit.Assert.assertEquals(message, expected.replaceAll("\\s+", " "),
                actual.replaceAll("\\s+", " "));
    }
}
