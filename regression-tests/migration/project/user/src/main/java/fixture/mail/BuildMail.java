package fixture.mail;
@org.springframework.stereotype.Component("fixture.mail.BuildMail")
public class BuildMail implements org.ikasan.spec.component.transformation.Converter<String,org.ikasan.component.endpoint.email.producer.EmailPayload> {
public org.ikasan.component.endpoint.email.producer.EmailPayload convert(String text) { var p=(org.ikasan.component.endpoint.email.producer.DefaultEmailPayload)org.ikasan.component.endpoint.email.producer.EmailPayload.newInstance(); p.setEmailBody(text);return p; }
}
