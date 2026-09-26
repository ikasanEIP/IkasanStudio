package org.ikasan.studio.verification;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/** Generated contract checks. No application startup, business input or external connections are inferred. */
public abstract class GeneratedVerificationSupport {
    /** SHA-256 of the saved model.json when this baseline was generated (UTF-8 bytes).
     * Identifies the model snapshot, not Java sources or compiled classes.
     * Refresh Verification Tests updates this value; running tests and migration do not.
     */
    private static final String BASELINE_MODEL_SHA256 = "${fingerprint}";

    @BeforeClass
    public static void reportBaselineStatus() throws Exception {
        Path model = Path.of("../generated/src/main/model/model.json");
        if (!Files.isRegularFile(model)) {
            System.out.println("NOT VERIFIED: current model fingerprint unavailable from this working directory");
            return;
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(model));
        StringBuilder hash = new StringBuilder();
        for (byte value : digest) hash.append(String.format("%02x", value & 255));
        if (!BASELINE_MODEL_SHA256.equals(hash.toString()))
            System.out.println("Verification baseline differs from current model. Expected during migration; tests remain frozen.");
    }

    /** Resolve types without invoking constructors or static initialisers. */
    private Class<?> load(String name) throws ClassNotFoundException {
        return Class.forName(name, false, getClass().getClassLoader());
    }

    /** The factory must expose a public no-argument method returning the declared interface. */
    protected final void assertFactory(String factory, String method, String contract) throws Exception {
        var type = load(factory);
        var factoryMethod = type.getMethod(method);
        assertTrue("Factory return contract: " + factory + "." + method,
                load(contract).isAssignableFrom(factoryMethod.getReturnType()));
    }

    /** Developer implementations must remain concrete and implement the snapshot's declared contract. */
    protected final void assertImplementation(String implementation, String contract) throws Exception {
        Class<?> type = load(implementation);
        assertFalse("Implementation must be concrete: " + implementation, Modifier.isAbstract(type.getModifiers()));
        assertTrue(implementation + " must implement " + contract, load(contract).isAssignableFrom(type));
    }
}
