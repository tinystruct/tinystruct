package org.tinystruct.system.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class FileFinder extends SimpleFileVisitor<Path> {
    final PathMatcher pathMatcher;

    public FileFinder(String pattern) {
        pathMatcher = FileSystems.getDefault().getPathMatcher(
                "glob:" + pattern);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if(pathMatcher.matches(file.getFileName())) {
            process(file);
        }
        return FileVisitResult.CONTINUE;
    }

    public abstract void process(Path file);

}
