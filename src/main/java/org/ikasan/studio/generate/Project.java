package org.ikasan.studio.generate;

/**
 * Generate the whole projet from the meta model and the current version of Ikasan
 */
public class Project {

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        String[] fmppArgs = new String[] {
                "-C", "/home/hidavi/dev/ws/tester/src/main/templates/config.fmpp",
                "-S", "/home/hidavi/dev/ws/tester/src/main/templates/com/",
                "-O", "/home/hidavi/dev/ws/tester/target/generated-sources/com/",
        };
        fmpp.tools.CommandLine.execute(fmppArgs, null, null);
    }

}
