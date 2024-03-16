package org.ikasan.studio.ui.model.ikasan.instance;

import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

//@Data
public class UiExceptionResolver extends UiBasicElement {
    protected AbstractViewHandler viewHandler;
    private ExceptionResolver exceptionResolver;

    public UiExceptionResolver(ExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }
}
