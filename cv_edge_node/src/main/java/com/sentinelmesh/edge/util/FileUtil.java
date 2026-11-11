package com.sentinelmesh.edge.util;

import java.nio.file.Path;
import java.util.Objects;

public final class FileUtil
{
    private FileUtil()
    {
    }

    /** Returns the absolute, normalized path without resolving symbolic links. */
    public static Path normalizeAbsolute(Path path)
    {
        return Objects.requireNonNull(path, "path cannot be null")
                .toAbsolutePath()
                .normalize();
    }

    /** Returns the final extension without the dot, or an empty string if none exists. */
    public static String getFileExtension(Path path)
    {
        Path fileName = Objects.requireNonNull(path, "path cannot be null").getFileName();
        if (fileName == null)
            return "";

        String name = fileName.toString();
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1)
            return "";

        return name.substring(dot + 1);
    }

    /** Matches the final extension without regard to case. The leading dot is optional. */
    public static boolean hasExtension(Path path, String extension)
    {
        String actual = getFileExtension(path);
        String expected = Objects.requireNonNull(extension, "extension cannot be null");
        if (expected.startsWith("."))
            expected = expected.substring(1);

        return !expected.isBlank() && actual.equalsIgnoreCase(expected);
    }

    /** Checks whether the candidate is exactly one path component below the directory. */
    public static boolean isDirectChildOf(Path candidate, Path directory)
    {
        Path parent = normalizeAbsolute(candidate).getParent();
        return normalizeAbsolute(directory).equals(parent);
    }

    /** Checks for a strict descendant by path components, without resolving symbolic links. */
    public static boolean isUnderDirectory(Path candidate, Path directory)
    {
        Path child = normalizeAbsolute(candidate);
        Path parent = normalizeAbsolute(directory);
        return !child.equals(parent) && child.startsWith(parent);
    }
}