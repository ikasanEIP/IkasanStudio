package org.ikasan.studio;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Unfortunately, the native logging used within an Intellij plugin project swallows
 * all system output (System.out and System.err) and LOGGING for test performance
 * purposes. This class re-enables the logging, otherwise the only option is to find
 * and tail the correct idea.log.
 */
// TODO ("Review if this is needed anymore.")
@TestInstance(Lifecycle.PER_CLASS)
public abstract class ConsoleOutputBaseTest {

    @BeforeAll
    public void setupLogging() {
        // Reattach System.out and System.err to real console streams (flush immediately)
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true));
    }

}