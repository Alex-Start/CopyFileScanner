package file;

import settings.PropertyReader;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FileComparator {
    private boolean isCheckSource = false;
    private String message;

    public FileComparator() {
    }

    public FileComparator(boolean isCheckSource) {
        this.isCheckSource = isCheckSource;
    }

    public boolean isMessage() {
        return message != null && !message.isEmpty();
    }

    public String getMessage() {
        return message;
    }

    private void setMessage(String message) {
        this.message = message;
    }

    /**
     * Return path and comment
     * @param sourceFiles - source files
     * @param destFiles - destination files
     * @return path, comment
     */
    public Map<String, String> compare(List<FileMetadata> sourceFiles, List<FileMetadata> destFiles) {
        Map<String, FileMetadata> sourceMap = new HashMap<>();
        Map<String, FileMetadata> destMap = new HashMap<>();

        // Store source files by relative path
        for (FileMetadata file : sourceFiles) {
            String relativePathAndFile = Path.of(file.getRelativePath().toString(), file.getAbsolutePath().getFileName().toString()).toString();
            sourceMap.put(relativePathAndFile, file);
        }

        // Store destination files by relative path
        for (FileMetadata file : destFiles) {
            String relativePathAndFile = Path.of(file.getRelativePath().toString(), file.getAbsolutePath().getFileName().toString()).toString();
            destMap.put(relativePathAndFile, file);
        }

        Map<String, String> differences = new HashMap<>();

        // Check for files missing in destination or having differences
        for (Map.Entry<String, FileMetadata> entry : sourceMap.entrySet()) {
            String relativePath = entry.getKey();
            FileMetadata sourceFile = entry.getValue();
            FileMetadata destFile = destMap.get(relativePath);

            boolean res = compare(sourceFile, destFile);
            if (!res) {
                differences.put(relativePath, getMessage());
            }
//            String fileComment = getSourceComment(sourceFile) + getDestinationComment(destFile);
//            if (destFile == null) {
//                differences.put(relativePath, "Missing in destination."+ fileComment);
//            } else {
//                if (sourceFile.getSize() != destFile.getSize()) {
//                    differences.put(relativePath, "Size mismatch."+ fileComment);
//                } else if (PropertyReader.getPropertyAsBoolean("compareByLastModified", false)
//                        && sourceFile.getLastModified() != destFile.getLastModified()) {
//                    differences.put(relativePath, "Last modified time mismatch."+ fileComment);
//                } else if (!Objects.equals(sourceFile.getChecksum(), destFile.getChecksum()) || sourceFile.getChecksum().isEmpty()) {
//                    //if error for getting checksum, then checksum is empty
//                    differences.put(relativePath, "Content mismatch."+ getSourceComment(sourceFile) + getDestinationComment(destFile));
//                }
//            }
        }

        if (isCheckSource) {
            // Check for files that exist in destination but are missing in source
            for (String relativePath : destMap.keySet()) {
                String fileComment = getDestinationComment(destMap.get(relativePath));

                if (!sourceMap.containsKey(relativePath)) {
                    differences.put(relativePath, "Missing in source."+ fileComment);
                }
            }
        }

        return differences;
    }

    public boolean compare(Path sourceFile, Path destFile) {
        return compare(FileMetadata.getFileMetadata(sourceFile), FileMetadata.getFileMetadata(destFile));
    }

    public boolean compare(FileMetadata sourceFile, FileMetadata destFile) {
        String fileComment = getSourceComment(sourceFile) + getDestinationComment(destFile);

        if (destFile == null) {
            setMessage("Missing in destination."+ fileComment);
            return false;
        } else {
            if (sourceFile == null) {
                setMessage("Missing in source."+ fileComment);
                return false;
            }
            if (sourceFile.getSize() != destFile.getSize()) {
                setMessage("Size mismatch."+ fileComment);
                return  false;
            } else if (PropertyReader.getPropertyAsBoolean("compareByLastModified", false)
                    && sourceFile.getLastModified() != destFile.getLastModified()) {
                setMessage("Last modified time mismatch."+ fileComment);
                return false;
            } else if (!Objects.equals(sourceFile.getChecksum(), destFile.getChecksum()) || sourceFile.getChecksum().isEmpty()) {
                //if error for getting checksum, then checksum is empty
                setMessage("Content mismatch."+ getSourceComment(sourceFile) + getDestinationComment(destFile));
                return false;
            }
        }

        return true;
    }

    private static String getSourceComment(FileMetadata sourceFile) {
        return getFileMetadatComment(sourceFile, " Source: ", ".");
    }

    private static String getDestinationComment(FileMetadata destFile) {
        return getFileMetadatComment(destFile, " Dest: ", ".");
    }

    private static String getFileMetadatComment(FileMetadata metadata, String prefix, String suffix) {
        if (metadata == null || metadata.getComment().isEmpty()) {
            return "";
        }
        return prefix + metadata.getComment() + suffix;
    }
}