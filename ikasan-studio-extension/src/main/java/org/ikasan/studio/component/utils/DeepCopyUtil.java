package org.ikasan.studio.component.utils;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class DeepCopyUtil {
    /**
     * Attempt to deep copy the supplied Object either by serialisation or reflection
     * @param original object to be duplicated
     * @return either a copy of the object or a reference to the object if copying was not possible
     * @param <T> type of object to be duplicated
     */
    public static <T> T deepCopy(T original) {
        if (original != null) {
            if (original instanceof Serializable) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(original);

                    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                    ObjectInputStream ois = new ObjectInputStream(bais);

                    return (T) ois.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    return original;
                }
            } else {
                T copy = null;
                try {
                    copy = (T) original.getClass().getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                    return original;
                }
                try {
                    for (Field field : original.getClass().getDeclaredFields()) {
                        field.setAccessible(true);
                            field.set(copy, field.get(original));
                    }
                } catch (IllegalAccessException e) {
                    return original;
                }
                return copy;
            }
        }
        return original;
    }
}