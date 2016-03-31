package de.qabel.qabelbox.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class PersistenceUtil {

    protected static String getTableNameForClass(Class cls) {
        return '\'' + cls.getCanonicalName() + '\'';
    }

    protected static byte[] serialize(String id, Serializable object) throws IllegalArgumentException {
        if (id != null && object != null) {
            if (id.length() == 0) {
                throw new IllegalArgumentException("ID cannot be empty!");
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                try {
                    ObjectOutputStream e = new ObjectOutputStream(baos);
                    e.writeObject(object);
                    e.close();
                } catch (IOException var5) {
                    throw new IllegalArgumentException("Cannot serialize object!", var5);
                }

                return baos.toByteArray();
            }
        } else {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
    }

    protected static Object deserialize(String id, byte[] input) throws IllegalArgumentException {
        if (id != null && input != null) {
            if (id.length() == 0) {
                throw new IllegalArgumentException("ID cannot be empty!");
            } else {
                try {
                    ByteArrayInputStream e = new ByteArrayInputStream(input);
                    ObjectInputStream ois = new ObjectInputStream(e);
                    Throwable var6 = null;

                    Object deserializedObject;
                    try {
                        deserializedObject = ois.readObject();
                    } catch (Throwable var16) {
                        var6 = var16;
                        throw var16;
                    } finally {
                        if (ois != null) {
                            if (var6 != null) {
                                try {
                                    ois.close();
                                } catch (Throwable var15) {
                                    var6.addSuppressed(var15);
                                }
                            } else {
                                ois.close();
                            }
                        }

                    }

                    return deserializedObject;
                } catch (IOException | ClassNotFoundException var18) {
                    throw new IllegalArgumentException("Cannot deserialize object!", var18);
                }
            }
        } else {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
    }
}
