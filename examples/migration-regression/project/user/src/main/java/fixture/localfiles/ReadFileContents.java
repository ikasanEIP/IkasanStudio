package fixture.localfiles;
@org.springframework.stereotype.Component("fixture.localfiles.ReadFileContents")
public class ReadFileContents implements org.ikasan.spec.component.transformation.Converter<java.util.List,String> {
public String convert(java.util.List paths) { try { var text=new StringBuilder();for(Object p:paths) text.append(java.nio.file.Files.readString(java.nio.file.Path.of(p.toString())));return text.toString(); }catch(java.io.IOException e){throw new org.ikasan.spec.component.transformation.TransformationException(e);} }
}
