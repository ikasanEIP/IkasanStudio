package org.ikasan;

/**
* <html><body><h2>Default List Splitter</h2><p>Splitters typically take in a single payload and split it into a number of smaller payloads that are sent to the downstream consumer one by one.</p><p>This splitter will take in a payload of 'fromType' and send multiple payloads of 'toType' to the downstream consumer.</p></body></html>
*
* This is an auto generated stub. The user is expected to fill in the details of the conversion below.
* This stub will not be over-written unless the overwrite checkbox is explicitly selected.
*/

import org.ikasan.spec.component.splitting.Splitter;
import org.ikasan.spec.component.splitting.SplitterException;

@org.springframework.stereotype.Component
public class MySplitter implements Splitter<java.lang.String, java.lang.String>
{
public java.util.List<java.lang.String> split(java.lang.String payload) throws SplitterException
{
java.util.List<java.lang.String> returnPayload = null;
// Your code here to generate a list of objects for the downstream from the single input object
return returnPayload;
}
}