#!/usr/bin/env python3
"""Compile and exercise exported recipe sources against locally cached, real Ikasan APIs.

First: STUDIO_RECIPE_EXPORT=/tmp/studio-recipe-sources ./gradlew test --tests '*ComposedConversionRecipeTest' --rerun-tasks
Then: python3 scripts/verify-conversion-recipes.py /tmp/studio-recipe-sources
Requires javac/java and the supported Ikasan artifacts in ~/.m2/repository; never downloads.
"""
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
EXPORT = Path(sys.argv[1])
M2 = Path.home() / '.m2/repository'

for pack, release, jms in [('V3.3.9', '11', 'javax.jms'), ('V4.1.6', '17', 'jakarta.jms')]:
    jars = list((M2 / 'org/ikasan').glob('*/' + pack[1:] + '/*.jar'))
    if not jars:
        raise SystemExit('Missing cached Ikasan artifacts for ' + pack)
    for location in ['org/springframework/spring-context', 'javax/jms', 'jakarta/jms',
                     'com/sun/mail', 'javax/activation', 'jakarta/activation']:
        jars += list((M2 / location).rglob('*.jar'))
    folder = EXPORT / pack
    classes = folder / 'classes'
    classes.mkdir(exist_ok=True)
    classpath = os.pathsep.join(map(str, jars))
    recipes = json.loads((ROOT / 'src/main/resources/studio/metapack' / pack /
                          'library/Converter/components/Converter/component-meta_en_GB.json').read_text())['conversionRecipes']
    sources = [folder / ('Recipe' + str(i) + '.java') for i in range(len(recipes))]
    subprocess.run(['javac', '-proc:none', '--release', release, '-cp', classpath, '-d', str(classes)]
                   + list(map(str, sources)), check=True)
    cases = '\n'.join('        check(%d, %s, %s, %s);' %
                      (i, json.dumps(r['sourceType']), json.dumps(r['targetType']), json.dumps(r['id']))
                      for i, r in enumerate(recipes))
    # A recipe with no possible upstream filename (plain String/byte[] source, not a Payload/local-file source
    # extract-file.ftl/extract-local-file.ftl would preserve a name from) - exercises the generated-filename
    # fallback in construct-file.ftl, including that it's unique under rapid/concurrent calls.
    uniqueness_index = next(i for i, r in enumerate(recipes)
                            if r['targetType'] == 'org.ikasan.filetransfer.Payload' and r['sourceType'] in ('java.lang.String', 'byte[]'))
    runner = r'''
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.lang.reflect.Proxy;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.ikasan.filetransfer.Payload;
import org.ikasan.filetransfer.component.DefaultPayload;
import org.ikasan.component.endpoint.email.producer.DefaultEmailPayload;

public class VerifyRecipes {
    static final byte[] CONTENT = "café".getBytes(StandardCharsets.UTF_8);
    static final Map<String,Object> MAP = Collections.singletonMap("key", "value");
    // Sentinel meaning "no upstream or explicitly-configured filename" - distinct from any real filename, so
    // verify() below knows to check the generated-fallback shape instead of an exact expected value.
    static final String GENERATED = "__GENERATED__";
    static final java.util.regex.Pattern GENERATED_FILE_PATTERN =
        java.util.regex.Pattern.compile(".*-\\d{8}-\\d{6}-\\d{3}-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.dat");
    static int checks;
    static void require(boolean result) { checks++; if (!result) throw new AssertionError(); }
    static void rejects(Converter converter, Object value) throws Exception {
        try { converter.convert(value); throw new AssertionError("Unsupported content was accepted"); }
        catch (TransformationException expected) { checks++; }
    }
    static Object message(String kind) throws Exception {
        Class<?> type = Class.forName("JMS." + kind);
        int[] offset = {0};
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getText": return "café";
                case "getMapNames": return Collections.enumeration(MAP.keySet());
                case "getObject": return MAP.get(args[0]);
                case "reset": offset[0] = 0; return null;
                case "getBodyLength": return (long) CONTENT.length;
                case "readBytes":
                    if (offset[0] == CONTENT.length) return -1;
                    int count = Math.min(2, CONTENT.length - offset[0]);
                    System.arraycopy(CONTENT, offset[0], (byte[]) args[0], 0, count);
                    offset[0] += count;
                    return count;
                default: throw new AssertionError("Unexpected JMS operation: " + method.getName());
            }
        });
    }
    static void verify(Object result, String target, String id, String filename) {
        if (target.equals("byte[]")) require(Arrays.equals(CONTENT, (byte[]) result));
        else if (target.equals("java.lang.String")) require("café".equals(result));
        else if (target.equals("java.util.Map")) require(MAP.equals(result));
        else if (result instanceof Payload) {
            require(Arrays.equals(CONTENT, ((Payload)result).getContent()));
            String actual = ((Payload)result).getAttribute("fileName");
            // FTP/SFTP: no upstream/explicit name means construct-file.ftl generated one - check its shape
            // (sanitised flow/converter name, timestamp, UUID) rather than an exact literal.
            if (filename.equals(GENERATED)) require(actual != null && GENERATED_FILE_PATTERN.matcher(actual).matches());
            else require(filename.equals(actual));
        } else {
            DefaultEmailPayload email = (DefaultEmailPayload) result;
            // Email attachment naming stays independent of the FTP/SFTP policy above - "message.dat" unless an
            // upstream/explicit name was preserved (see construct-email-attachment.ftl).
            String attachmentFilename = filename.equals(GENERATED) ? "message.dat" : filename;
            if (id.endsWith("email-attachment")) {
                require(Arrays.equals(CONTENT, email.getAttachment(attachmentFilename)));
                require("application/octet-stream".equals(email.getAttachmentType(attachmentFilename)));
                require("Please see the attached file.".equals(email.getEmailBody()));
            } else require("café".equals(email.getEmailBody()));
        }
    }
    static void check(int index, String source, String target, String id) throws Exception {
        Converter converter = (Converter) Class.forName("org.ikasan.Recipe" + index).getConstructor().newInstance();
        Object input;
        String filename = GENERATED;
        Path file = null;
        if (source.equals("java.lang.String")) input = "café";
        else if (source.equals("byte[]")) input = CONTENT;
        else if (source.equals("java.lang.Object")) input = target.equals("java.util.Map") ? MAP : CONTENT;
        else if (source.contains("jms.Message")) input = message(target.equals("java.util.Map") ? "MapMessage" : "TextMessage");
        else if (source.contains("java.util.List")) {
            file = Files.createTempFile("studio-recipe-", ".txt");
            Files.write(file, CONTENT);
            filename = file.getFileName().toString();
            input = Collections.singletonList(file.toFile());
        } else {
            DefaultPayload payload = new DefaultPayload("id", CONTENT);
            payload.setAttribute("fileName", "source.txt");
            filename = "source.txt";
            input = payload;
        }
        try {
            verify(converter.convert(input), target, id, filename);
            rejects(converter, null);
            if (source.equals("java.lang.Object")) rejects(converter, new Object());
            if (source.contains("java.util.List")) rejects(converter, Arrays.asList(file.toFile(), file.toFile()));
            if (source.contains("jms.Message")) {
                rejects(converter, message("ObjectMessage"));
                if (!target.equals("java.util.Map")) {
                    verify(converter.convert(message("BytesMessage")), target, id, filename);
                    rejects(converter, message("MapMessage"));
                }
            }
            if (source.equals("byte[]") && (target.equals("java.lang.String") || id.endsWith("email-payload")))
                rejects(converter, new byte[]{(byte)0xff});
        } finally { if (file != null) Files.deleteIfExists(file); }
    }
    // Rapid and concurrent calls must never generate the same fallback filename - collectors like a strict FTP
    // test emulator refuse to overwrite an existing file, so a collision here would stop the flow just like the
    // fixed "message.dat" default this replaced.
    static void checkUniqueness(int index) throws Exception {
        Converter converter = (Converter) Class.forName("org.ikasan.Recipe" + index).getConstructor().newInstance();
        String first = ((Payload) converter.convert("café")).getAttribute("fileName");
        String second = ((Payload) converter.convert("café")).getAttribute("fileName");
        require(!first.equals(second));
        int threadCount = 8;
        Set<String> names = Collections.synchronizedSet(new HashSet<>());
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try { names.add(((Payload) converter.convert("café")).getAttribute("fileName")); }
                catch (Throwable t) { failures.add(t); }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        require(failures.isEmpty());
        require(names.size() == threadCount);
    }
    public static void main(String[] args) throws Exception {
CASES
        checkUniqueness(UNIQUENESS_INDEX);
        System.out.println("Runtime assertions passed: " + checks);
    }
}
'''.replace('JMS.', jms + '.').replace('CASES', cases).replace('UNIQUENESS_INDEX', str(uniqueness_index))
    runner_file = folder / 'VerifyRecipes.java'
    runner_file.write_text(runner)
    classpath = str(classes) + os.pathsep + classpath
    subprocess.run(['javac', '-proc:none', '--release', release, '-cp', classpath, '-d', str(classes), str(runner_file)], check=True)
    print(pack + ': compiled ' + str(len(sources)) + ' converters', flush=True)
    subprocess.run(['java', '-cp', classpath, 'VerifyRecipes'], check=True)
