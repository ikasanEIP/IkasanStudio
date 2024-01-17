package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import javax.swing.*;
import java.util.Map;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
public class IkasanComponentMetan {
    private static final String DEFAULT_README = "Readme.md";
    String name;
    String helpText;
    String componentType;
    String implementingClass;
    String ikasanComponentFactoryMethod;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    @Builder.Default
    String webHelpURL = DEFAULT_README;
    String smallPalletteIcon;
    String mediumPalletteIcon;
    String largePalletteIcon;

    ImageIcon smallIcon;
    ImageIcon canvasIcon;

    Map<String, IkasanComponentPropertyMetan> properties;
    //Map<String, IkasanComponentPropertyMetan> componentProperties = new LinkedHashMap<>();
}
