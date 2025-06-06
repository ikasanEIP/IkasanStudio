package org.ikasan.studio.component.utils.fixtures;

import java.io.Serializable;
import java.util.Objects;

public class TestDeepCopyDataSerializable implements Serializable {
    String strAttribute1;
    Integer intAttribute1;

    public TestDeepCopyDataSerializable(String strAttribute1, Integer intAttribute1) {
        this.strAttribute1 = strAttribute1;
        this.intAttribute1 = intAttribute1;
    }

    public String getStrAttribute1() {
        return strAttribute1;
    }

    public Integer getIntAttribute1() {
        return intAttribute1;
    }

    public void setIntAttribute1(Integer intAttribute1) {
        this.intAttribute1 = intAttribute1;
    }

    public void setStrAttribute1(String strAttribute1) {
        this.strAttribute1 = strAttribute1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestDeepCopyDataSerializable)) return false;
        TestDeepCopyDataSerializable that = (TestDeepCopyDataSerializable) o;
        return Objects.equals(strAttribute1, that.strAttribute1) && Objects.equals(intAttribute1, that.intAttribute1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strAttribute1, intAttribute1);
    }
}
