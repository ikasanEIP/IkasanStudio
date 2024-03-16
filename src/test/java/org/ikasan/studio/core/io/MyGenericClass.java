package org.ikasan.studio.core.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
public class MyGenericClass {
    public String field1String;
    public Boolean field1Boolean;
    public Integer field1Integer;
    public Long field1Long;
    public Double field1Double;
    public Float field1Float;
    MyGenericOtherClass field1Subclass;

    public String unsuppliedField1String;
    public Boolean unsuppliedField1Boolean;
    public Integer unsuppliedField1Integer;
    public Long unsuppliedField1Long;
    public Double unsuppliedField1Double;
    public Float unsuppliedField1Float;
    MyGenericOtherClass unsuppliedField1Subclass;

    public boolean unsuppliedPrimitiveField1Boolean;
    public int unsuppliedPrimitiveField1Int;
    public long unsuppliedPrimitiveField1Long;
    public double unsuppliedPrimitiveField1Double;
    public float unsuppliedPrimitiveField1Float;

    Map<String, MyGenericOtherClass> field1Map;
}
