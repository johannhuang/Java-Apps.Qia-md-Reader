package de.qiasoft.qiamdreader;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

import javafx.stage.FileChooser;

public class FileHelper {
    public static final String[] SUPPORTED_EXTENSIONS = new String[] {".md", ".html", ".txt"};

    public static String stripLeadingFileSeparator(String pathS) {
        String fileSeparator = File.separator;
        if (pathS.startsWith(fileSeparator)) {
            return pathS.substring(fileSeparator.length());
        }
        return pathS;
    }

    public static String joinPaths(String pathS1, String pathS2) {
        return Paths.get(pathS1).resolve(Paths.get(pathS2)).toString();
    }

    public static Boolean isSupportedType(final File file) {
        return Stream.of(SUPPORTED_EXTENSIONS).anyMatch(ext -> file.getAbsolutePath().endsWith(ext));
    }

    public static Boolean isSupportedType(final String pathS) {
        return Stream.of(SUPPORTED_EXTENSIONS).anyMatch(pathS::endsWith);
    }

    public static File getFile(String pathS) {
        if (Stream.of(SUPPORTED_EXTENSIONS).anyMatch(pathS::endsWith)) {
            File file = new File(pathS);
            if (file.exists() && file.isFile()) {
                return file;
            }
        }
        return null;
    }

    public static String getFileText(String pathS) {
        try {
            return Files.readString(Paths.get(pathS));
        } catch (IOException e) {
            QiaLogging.logger.log(Level.INFO, "FileHelper::static.getFileText::IOException: " + e);
        }
        return "";
    }

    public static File chooseFileFromStorage(final File base) {
        FileChooser fileChooser = new FileChooser();

        if (base != null) {
            if (base.isDirectory()) fileChooser.setInitialDirectory(base);
            else fileChooser.setInitialDirectory(base.getParentFile());
        } else {
            File dir = new File(System.getProperty("user.home"));
            if (dir.exists() && dir.isDirectory()) {
                fileChooser.setInitialDirectory(dir);
            }
        }

        for (String extension : SUPPORTED_EXTENSIONS) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                String.format(Locale.ROOT, "%s file type", extension), "*" + extension
            ));
        }

        return fileChooser.showOpenDialog(null);
    }

    public static Set<String> getFilePathSet(String dirPath) {
        Set<String> filePaths = new HashSet<>();
        try {
            Files.walkFileTree(Paths.get(dirPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    String pathS = path.toString();
                    if (Stream.of(SUPPORTED_EXTENSIONS).anyMatch(pathS::endsWith)) filePaths.add(pathS);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            QiaLogging.logger.log(Level.INFO, "FileHelper::static.getFilePaths::IOException: " + e);
        }
        return filePaths;
    }
}
