package fixture.verification;

import java.nio.file.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.ftpserver.*;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.*;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.common.config.keys.PublicKeyEntry;

/** Loopback-only disposable service fixtures. Never touches developer servers or SSH configuration. */
public final class LocalServices implements AutoCloseable {
    private FtpServer ftp; private SshServer sftp; private ServerSocket smtp; private Thread smtpThread;
    final List<String> mail=new CopyOnWriteArrayList<>();
    final List<String> arguments=new ArrayList<>();
    LocalServices() throws Exception {
        try { start(); } catch(Exception e){close();throw e;}
    }
    private void start() throws Exception {
        Path root=Files.createDirectories(Path.of("services").toAbsolutePath());
        Path ftpHome=Files.createDirectories(root.resolve("ftp"));Files.createDirectories(ftpHome.resolve("upload"));
        var factory=new FtpServerFactory();var listener=new ListenerFactory();
        listener.setServerAddress("127.0.0.1");listener.setPort(Boolean.getBoolean("fixture.fixedPorts")?2121:freePort());factory.addListener("default",listener.createListener());
        var user=new BaseUser();user.setName("fixture");user.setPassword("fixture");user.setHomeDirectory(ftpHome.toString());user.setAuthorities(List.of(new WritePermission()));factory.getUserManager().save(user);
        ftp=factory.createServer();ftp.start();
        int ftpPort=((org.apache.ftpserver.impl.DefaultFtpServer)ftp).getListener("default").getPort();
        for(String flow:List.of("ftpsender","ftpreceiver")) arguments.add("--"+flow+".ftp.consumer.remote-port="+ftpPort);
        Path sftpHome=Files.createDirectories(root.resolve("sftp"));Files.createDirectories(sftpHome.resolve("upload"));
        sftp=SshServer.setUpDefaultServer();sftp.setHost("127.0.0.1");sftp.setPort(Boolean.getBoolean("fixture.fixedPorts")?2222:0);
        var hostKey=new SimpleGeneratorHostKeyProvider(root.resolve("host-key.ser"));hostKey.setAlgorithm("RSA");hostKey.setKeySize(2048);
        sftp.setKeyPairProvider(hostKey);
        sftp.setSignatureFactories(List.of(org.apache.sshd.common.signature.BuiltinSignatures.rsaSHA512, org.apache.sshd.common.signature.BuiltinSignatures.rsaSHA256, org.apache.sshd.common.signature.BuiltinSignatures.rsa));
        sftp.setPasswordAuthenticator((name,password,session)->"fixture".equals(name)&&"fixture".equals(password));
        sftp.setPublickeyAuthenticator((name,key,session)->false);
        sftp.setSubsystemFactories(List.of(new SftpSubsystemFactory.Builder().build()));
        sftp.setFileSystemFactory(new VirtualFileSystemFactory(sftpHome));sftp.start();
        String publicKey=PublicKeyEntry.toString(hostKey.loadKeys(null).iterator().next().getPublic());
        Path hosts=root.resolve("known_hosts");Files.writeString(hosts,"[127.0.0.1]:"+sftp.getPort()+" "+publicKey+"\n");
        // An empty key path selects password authentication; no personal SSH key is consulted.
        arguments.add("--fixture.sftp.key=");arguments.add("--fixture.sftp.knownHosts="+hosts);
        for(String flow:List.of("sftpsender","sftpreceiver")) arguments.add("--"+flow+".sftp.consumer.remote-port="+sftp.getPort());
        smtp=new ServerSocket(Boolean.getBoolean("fixture.fixedPorts")?2525:0,8,InetAddress.getLoopbackAddress());smtp.setSoTimeout(500);
        arguments.add("--mail.email.producer.mail-smtp-port="+smtp.getLocalPort());
        smtpThread=new Thread(this::serveMail,"fixture-smtp");smtpThread.setDaemon(true);smtpThread.start();
    }
    int smtpPort(){return smtp.getLocalPort();}
    private static int freePort() throws IOException { try(var socket=new ServerSocket(0,8,InetAddress.getLoopbackAddress())){return socket.getLocalPort();} }
    public static void main(String[] args) throws Exception {
        System.setProperty("fixture.fixedPorts","true");
        LocalServices services=new LocalServices();
        Runtime.getRuntime().addShutdownHook(new Thread(()->{try{services.close();}catch(Exception e){e.printStackTrace();}}));
        Files.createDirectories(Path.of("inputs"));
        Path sample=Path.of("inputs/interactive.txt");
        if(!Files.exists(sample))Files.writeString(sample,"LOCAL-FIRST\n");
        System.out.println("Fixture services ready on loopback: FTP 2121, SFTP 2222, SMTP 2525. Login fixture / fixture.");
        System.out.println("Start the module in Studio. Leave this process running; Ctrl+C stops only these test services.");
        new java.util.concurrent.CountDownLatch(1).await();
    }
    private void serveMail(){
        while(!smtp.isClosed())try(var client=smtp.accept()) {
            client.setSoTimeout(5000);
            var in=new BufferedReader(new InputStreamReader(client.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));
            var out=new PrintWriter(client.getOutputStream(),true);out.print("220 fixture ESMTP\r\n");out.flush();
            String line;boolean data=false;StringBuilder message=new StringBuilder();
            while((line=in.readLine())!=null){
                if(data){if(line.equals(".")){mail.add(message.toString());data=false;out.print("250 stored\r\n");}else message.append(line).append('\n');}
                else if(line.startsWith("DATA")){data=true;message.setLength(0);out.print("354 end with dot\r\n");}
                else if(line.startsWith("QUIT")){out.print("221 bye\r\n");out.flush();break;}
                else out.print("250 OK\r\n");
                out.flush();
            }
        }catch(java.net.SocketTimeoutException ignored){}catch(IOException e){if(!smtp.isClosed())mail.add("SERVER ERROR: "+e);}
    }
    public void close() throws Exception {
        if(smtp!=null)smtp.close();if(smtpThread!=null)smtpThread.join(6000);
        if(sftp!=null)sftp.stop(true);if(ftp!=null)ftp.stop();
    }
}
