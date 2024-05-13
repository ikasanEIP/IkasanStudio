package ${studioPackageTag};

/**
* The only purpose of this component is to allow the developer to set breakpoints inbetween components.
* The components will not be saved away when the project closes, do not add any code here you wish to keep
*/

import org.ikasan.spec.component.filter.Filter;
import org.ikasan.spec.component.filter.FilterException;

public abstract class ${className} implements Filter<java.lang.Object> {

@Override
public final Object filter(Object message) throws FilterException {
debug(message);
return message;
}

public abstract void debug(Object message) ;
}
