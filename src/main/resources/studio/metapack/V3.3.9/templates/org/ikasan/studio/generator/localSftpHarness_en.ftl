    /** Development-only override supplied by Studio's local SFTP harness. External configuration stays unchanged. */
    private static <T> T studioLocalSftp(T endpoint, String flow, String component, boolean consumer) {
        if (!Boolean.getBoolean("studio.test.sftp.enabled")) return endpoint;
        try {
            Object configuration = endpoint.getClass().getMethod("getConfiguration").invoke(endpoint);
            String directory = "flow-" + java.net.URLEncoder.encode(flow, java.nio.charset.StandardCharsets.UTF_8)
                    + "/component-" + java.net.URLEncoder.encode(component, java.nio.charset.StandardCharsets.UTF_8);
            // Flow startup reloads persisted configuration. Give the local run a separate ID so
            // external server settings cannot overwrite these values, or be overwritten by them.
            endpoint.getClass().getMethod("setConfiguredResourceId", String.class).invoke(endpoint,
                    "studio-test-sftp-" + System.getProperty("studio.test.sftp.session") + "-" + directory);
            java.nio.file.Path home = java.nio.file.Path.of(System.getProperty("studio.test.sftp.home"));
            java.nio.file.Files.createDirectories(home.resolve(directory));
            studioSftpSetting(configuration, "RemoteHost", String.class, "127.0.0.1");
            studioSftpSetting(configuration, "RemotePort", Integer.class, Integer.valueOf(System.getProperty("studio.test.sftp.port")));
            studioSftpSetting(configuration, "Username", String.class, "ikasan");
            studioSftpSetting(configuration, "Password", String.class, System.getProperty("studio.test.sftp.password"));
            studioSftpSetting(configuration, "PrivateKeyFilename", String.class, "");
            studioSftpSetting(configuration, "PrivateKeyPassphrase", String.class, "");
            studioSftpSetting(configuration, "KnownHostsFilename", String.class, System.getProperty("studio.test.sftp.knownHosts"));
            studioSftpSetting(configuration, "PreferredKeyExchangeAlgorithm", String.class, null);
            studioSftpSetting(configuration, consumer ? "SourceDirectory" : "OutputDirectory", String.class, "/" + directory);
            if (consumer) studioSftpSetting(configuration, "MinAge", Long.class, 0L);
            return endpoint;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot configure local test SFTP server for " + flow + " / " + component, failure);
        }
    }
    private static void studioSftpSetting(Object configuration, String name, Class<?> type, Object value) throws Exception {
        configuration.getClass().getMethod("set" + name, type).invoke(configuration, value);
    }
