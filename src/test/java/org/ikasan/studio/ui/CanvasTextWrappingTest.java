package org.ikasan.studio.ui;

import org.junit.jupiter.api.Test;
import java.awt.Font;
import java.awt.image.BufferedImage;
import static org.assertj.core.api.Assertions.assertThat;

class CanvasTextWrappingTest {
    @Test void longIdentifiersAndEndpointNamesFitTheirMeasuredWidth() {
        var graphics = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            for (int size : new int[]{12, 18, 24}) {
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
                for (String text : new String[]{"org.example.endpoint.producer.CustomInstrumentProducer",
                        "dynamicQueues/example.instrument.publisher", "Long converter name with several words", "注文変換コンポーネント𠀀"}) {
                    var lines = StudioUIUtils.wrapTextToWidth(graphics.getFontMetrics(), text, 90);
                    assertThat(lines).allSatisfy(line -> assertThat(graphics.getFontMetrics().stringWidth(line)).isLessThanOrEqualTo(90));
                    assertThat(String.join("", lines).replace(" ", "")).isEqualTo(text.replace(" ", ""));
                    int measured = StudioUIUtils.drawCenteredStringFromTopCentre(graphics, PaintMode.DIMENSION_ONLY, text, 500, 10, 90, null);
                    int painted = StudioUIUtils.drawCenteredStringFromTopCentre(graphics, PaintMode.PAINT, text, 500, 10, 90, null);
                    assertThat(painted).isEqualTo(measured);
                }
            }
        } finally { graphics.dispose(); }
    }
}
