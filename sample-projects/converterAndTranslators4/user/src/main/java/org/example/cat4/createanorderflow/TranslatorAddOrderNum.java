package org.example.cat4.createanorderflow;

/**
 * The main responsibility of a translator is to translate the payload (not the type).
 * <p>
 * Note: translate() below always receives just the payload, never the full FlowEvent - unlike Broker/Converter,
 * there is no full-event mode to opt into here. If your translation logic needs the event's identifier,
 * timestamp or other metadata, a Broker (with its input type set to org.ikasan.spec.flow.FlowEvent) is a
 * better fit.
 * <p>
 * This is an auto generated stub. The user is expected to fill in the details of the conversion below.
 * This stub will not be over-written unless the overwrite checkbox is explicitly selected.
 */

import org.example.cat4.domain.Order;
import org.ikasan.spec.component.transformation.Translator;
import org.ikasan.spec.component.transformation.TransformationException;
import org.springframework.stereotype.Component;

@Component("org.example.cat4.createanorderflow.TranslatorAddOrderNum")
public class TranslatorAddOrderNum implements Translator<Order> {
    /**
     * Transforms or otherwise changes the passed in <code>Event</code>
     *
     * @param payload Event to transform
     * @throws TransformationException Exception if we could not transform
     */
    private static int orderNum=100;
    public void translate(Order payload) throws TransformationException {
        payload.setOrderNumber(orderNum++);
    }
}