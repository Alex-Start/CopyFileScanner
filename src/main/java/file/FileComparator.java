package file;

import settings.PropertyReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FileComparator {
    private boolean isCheckSource = false;

    public FileComparator() {
    }

    public FileComparator(boolean isCheckSource) {
        this.isCheckSource = isCheckSource;
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
            sourceMap.put(file.getRelativePath(), file);
        }

        // Store destination files by relative path
        for (FileMetadata file : destFiles) {
            destMap.put(file.getRelativePath(), file);
        }

        Map<String, String> differences = new HashMap<>();

        // Check for files missing in destination or having differences
        for (Map.Entry<String, FileMetadata> entry : sourceMap.entrySet()) {
            String relativePath = entry.getKey();
            FileMetadata sourceFile = entry.getValue();
            FileMetadata destFile = destMap.get(relativePath);
            String fileComment = getSourceComment(sourceFile) + getDestinationComment(destFile);

            if (destFile == null) {
                differences.put(relativePath, "Missing in destination."+ fileComment);
            } else {
                if (sourceFile.getSize() != destFile.getSize()) {
                    differences.put(relativePath, "Size mismatch."+ fileComment);
                } else if (PropertyReader.getPropertyAsBoolean("compareByLastModified", false)
                        && sourceFile.getLastModified() != destFile.getLastModified()) {
                    differences.put(relativePath, "Last modified time mismatch."+ fileComment);
                } else if (!Objects.equals(sourceFile.getChecksum(), destFile.getChecksum()) || sourceFile.getChecksum().isEmpty()) {
                    //if error for getting checksum, then checksum is empty
                    differences.put(relativePath, "Content mismatch."+ getSourceComment(sourceFile) + getDestinationComment(destFile));
                }
            }
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

    private String getSourceComment(FileMetadata sourceFile) {
        return getFileMetadatComment(sourceFile, " Source: ", ".");
    }

    private String getDestinationComment(FileMetadata destFile) {
        return getFileMetadatComment(destFile, " Dest: ", ".");
    }

    private String getFileMetadatComment(FileMetadata metadata, String prefix, String suffix) {
        if (metadata == null || metadata.getComment().isEmpty()) {
            return "";
        }
        return prefix + metadata.getComment() + suffix;
    }
}