package org.ikasan.studio.core.persistence.json;

/** Exact corrections for implementation identifiers in previously saved Studio documents only. */
public final class ComponentImplementationAliases {
    private ComponentImplementationAliases() { }

    public static String canonical(String implementation) {
        return "org.ikasan.builder.component.converter.ObjectToXmlStringConverterBuilder".equals(implementation)
                ? "org.ikasan.component.converter.xml.ObjectToXMLStringConverter" : implementation;
    }
}
