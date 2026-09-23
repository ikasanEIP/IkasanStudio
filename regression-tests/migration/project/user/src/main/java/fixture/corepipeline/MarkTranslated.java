package fixture.corepipeline;
@org.springframework.stereotype.Component("fixture.corepipeline.MarkTranslated")
public class MarkTranslated implements org.ikasan.spec.component.transformation.Translator<fixture.Order> {
public void translate(fixture.Order o) { o.setStage("translated"); }
}
