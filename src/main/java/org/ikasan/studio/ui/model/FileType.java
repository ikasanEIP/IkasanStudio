package org.ikasan.studio.ui.model;

public enum FileType {
    XML("xml", "XML File"),
    JAVA("java", "Java Source File"),
    PROPERTIES("properties", "Properties File");

    private final String extension;
    private final String description;

    FileType(String extension, String description) {
        this.extension = extension;
        this.description = description;
    }

    public String getExtension() {
        return extension;
    }

    public String getDescription() {
        return description;
    }

    public static FileType fromFileName(String fileName) {
        if (fileName == null && fileName.contains(".")) {
            return FileType.fromExtension(fileName.substring(fileName.lastIndexOf(".") + 1));
        }
        return PROPERTIES;
    }

    public static FileType fromExtension(String extension) {
        if (extension != null) {
            for (FileType fileType : values()) {
                if (fileType.extension.equalsIgnoreCase(extension)) {
                    return fileType;
                }
            }
        }
        return PROPERTIES;
    }
}
