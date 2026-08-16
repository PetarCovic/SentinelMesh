package com.sentinelmesh.edge.recording;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import com.sentinelmesh.edge.exceptions.RecordingSpoolException;

public class RecordingSpoolMetadataStore
{
    private static final String SEGMENT_ID = "segmentId";
    private static final String DEVICE_ID = "deviceId";
    private static final String VIDEO_FILE_NAME = "videoFileName";
    private static final String SEGMENT_START_TIME = "segmentStartTime";
    private static final String SEGMENT_END_TIME = "segmentEndTime";
    private static final String FILE_SIZE_BYTES = "fileSizeBytes";
    private static final String UPLOAD_ATTEMPTS = "uploadAttempts";
    private static final String STATUS = "status";
    private static final String CREATED_AT = "createdAt";
    private static final String LAST_UPLOAD_ERROR = "lastUploadError";

    public void save(RecordingSpoolEntry entry)
    {
        if(entry == null)
        {
            throw new IllegalArgumentException(
                    "RecordingSpoolEntry cannot be null"
            );
        }

        Path metadataPath =
                getMetadataPath(entry.getVideoPath());

        Path tempPath =
                metadataPath.resolveSibling(
                        metadataPath.getFileName().toString() + ".tmp"
                );

        Properties properties = new Properties();

        properties.setProperty(
                SEGMENT_ID,
                entry.getSegmentId().toString()
        );

        properties.setProperty(
                DEVICE_ID,
                entry.getDeviceId().toString()
        );

        properties.setProperty(
                VIDEO_FILE_NAME,
                entry.getVideoPath()
                        .toAbsolutePath()
                        .normalize()
                        .getFileName()
                        .toString()
        );

        properties.setProperty(
                SEGMENT_START_TIME,
                entry.getSegmentStartTime().toString()
        );

        properties.setProperty(
                SEGMENT_END_TIME,
                entry.getSegmentEndTime().toString()
        );

        properties.setProperty(
                FILE_SIZE_BYTES,
                Long.toString(entry.getFileSizeBytes())
        );

        properties.setProperty(
                UPLOAD_ATTEMPTS,
                Integer.toString(entry.getUploadAttempts())
        );

        properties.setProperty(
                STATUS,
                entry.getStatus().name()
        );

        properties.setProperty(
                CREATED_AT,
                entry.getCreatedAt().toString()
        );

        if(entry.getLastUploadError() != null)
        {
            properties.setProperty(
                    LAST_UPLOAD_ERROR,
                    entry.getLastUploadError()
            );
        }
        
        properties.setProperty(
        		"cleanupAttempts",
        		Integer.toString(entry.getCleanupAttempts())
        );

        if(entry.getNextCleanupAttemptAt() != null)
        {
        	properties.setProperty(
        			"nextCleanupAttemptAt",
        			entry.getNextCleanupAttemptAt().toString()
        	);
        }

        if(entry.getLastCleanupError() != null)
        {
        	properties.setProperty(
        			"lastCleanupError",
        			entry.getLastCleanupError()
        	);
        }

        try
        {
            Files.createDirectories(metadataPath.getParent());

            try(OutputStream outputStream =
                    Files.newOutputStream(
                            tempPath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    ))
            {
                properties.store(
                        outputStream,
                        "SentinelMesh recording spool metadata"
                );
            }

            moveTemporaryMetadata(
                    tempPath,
                    metadataPath
            );
        }
        catch(IOException ex)
        {
            try
            {
                Files.deleteIfExists(tempPath);
            }
            catch(IOException cleanupException)
            {
                ex.addSuppressed(cleanupException);
            }

            throw new RecordingSpoolException(
                    "Failed to save recording metadata: "
                            + metadataPath,
                    ex
            );
        }
    }

    public RecordingSpoolEntry load(Path metadataPath)
    {
        Path normalizedMetadataPath =
                validateMetadataPath(metadataPath);

        if(!Files.isRegularFile(normalizedMetadataPath))
        {
            throw new RecordingSpoolException(
                    "Recording metadata path is not a file: "
                            + normalizedMetadataPath
            );
        }

        Properties properties = new Properties();

        try(InputStream inputStream =
                Files.newInputStream(normalizedMetadataPath))
        {
            properties.load(inputStream);

            UUID segmentId =
                    UUID.fromString(
                            getRequiredProperty(
                                    properties,
                                    SEGMENT_ID
                            )
                    );

            UUID deviceId =
                    UUID.fromString(
                            getRequiredProperty(
                                    properties,
                                    DEVICE_ID
                            )
                    );

            String videoFileName =
                    getRequiredProperty(
                            properties,
                            VIDEO_FILE_NAME
                    );

            Path metadataDirectory =
                    normalizedMetadataPath.getParent();

            Path videoPath =
                    metadataDirectory
                            .resolve(videoFileName)
                            .normalize();

            if(!videoPath.startsWith(metadataDirectory))
            {
                throw new RecordingSpoolException(
                        "Video path escapes the metadata directory: "
                                + videoFileName
                );
            }

            Instant segmentStartTime =
                    Instant.parse(
                            getRequiredProperty(
                                    properties,
                                    SEGMENT_START_TIME
                            )
                    );

            Instant segmentEndTime =
                    Instant.parse(
                            getRequiredProperty(
                                    properties,
                                    SEGMENT_END_TIME
                            )
                    );

            long fileSizeBytes =
                    Long.parseLong(
                            getRequiredProperty(
                                    properties,
                                    FILE_SIZE_BYTES
                            )
                    );

            int uploadAttempts =
                    Integer.parseInt(
                            getRequiredProperty(
                                    properties,
                                    UPLOAD_ATTEMPTS
                            )
                    );

            RecordingSpoolStatus status =
                    RecordingSpoolStatus.valueOf(
                            getRequiredProperty(
                                    properties,
                                    STATUS
                            )
                    );

            Instant createdAt =
                    Instant.parse(
                            getRequiredProperty(
                                    properties,
                                    CREATED_AT
                            )
                    );

            String lastUploadError =
                    properties.getProperty(
                            LAST_UPLOAD_ERROR
                    );
            
            int cleanupAttempts = Integer.parseInt(
            		properties.getProperty(
            				"cleanupAttempts",
            				"0"
            		)
            );

            Instant nextCleanupAttemptAt =
            		parseOptionalInstant(
            				properties.getProperty(
            						"nextCleanupAttemptAt"
            				)
            		);

            String lastCleanupError =
            		normalizeOptionalProperty(
            				properties.getProperty(
            						"lastCleanupError"
            				)
            		);

            return new RecordingSpoolEntry(
                    segmentId,
                    deviceId,
                    videoPath,
                    segmentStartTime,
                    segmentEndTime,
                    fileSizeBytes,
                    uploadAttempts,
                    status,
                    createdAt,
                    lastUploadError,
                    cleanupAttempts,
                    nextCleanupAttemptAt,
                    lastCleanupError
            );
        }
        catch(IOException | IllegalArgumentException ex)
        {
            throw new RecordingSpoolException(
                    "Failed to load recording metadata: "
                            + normalizedMetadataPath,
                    ex
            );
        }
    }

    public List<RecordingSpoolEntry> loadAll(Path directory)
    {
        if(directory == null
                || directory.toString().isBlank())
        {
            throw new IllegalArgumentException(
                    "Directory cannot be null or blank"
            );
        }

        Path normalizedDirectory =
                directory.toAbsolutePath().normalize();

        if(!Files.isDirectory(normalizedDirectory))
        {
            throw new RecordingSpoolException(
                    "Recording metadata directory does not exist: "
                            + normalizedDirectory
            );
        }

        try(Stream<Path> paths = Files.list(normalizedDirectory))
        {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isMetadataFile)
                    .map(this::load)
                    .sorted(
                            (first, second) ->
                                    first.getSegmentStartTime()
                                            .compareTo(
                                                    second.getSegmentStartTime()
                                            )
                    )
                    .toList();
        }
        catch(IOException ex)
        {
            throw new RecordingSpoolException(
                    "Failed to load recording metadata from directory: "
                            + normalizedDirectory,
                    ex
            );
        }
    }

    public boolean delete(Path metadataPath)
    {
        Path normalizedMetadataPath =
                validateMetadataPath(metadataPath);

        try
        {
            return Files.deleteIfExists(
                    normalizedMetadataPath
            );
        }
        catch(IOException ex)
        {
            throw new RecordingSpoolException(
                    "Failed to delete recording metadata: "
                            + normalizedMetadataPath,
                    ex
            );
        }
    }

    public Path getMetadataPath(Path videoPath)
    {
        if(videoPath == null
                || videoPath.toString().isBlank())
        {
            throw new IllegalArgumentException(
                    "VideoPath cannot be null or blank"
            );
        }

        Path normalizedVideoPath =
                videoPath.toAbsolutePath().normalize();

        Path fileNamePath =
                normalizedVideoPath.getFileName();

        Path parent =
                normalizedVideoPath.getParent();

        if(fileNamePath == null || parent == null)
        {
            throw new IllegalArgumentException(
                    "VideoPath must contain a filename and parent directory"
            );
        }

        String fileName =
                fileNamePath.toString();

        if(!fileName.toLowerCase().endsWith(".mp4"))
        {
            throw new IllegalArgumentException(
                    "VideoPath must be an MP4 file"
            );
        }

        String metadataFileName =
                fileName.substring(
                        0,
                        fileName.length() - ".mp4".length()
                )
                + ".properties";

        Path metadataPath =
                parent.resolve(metadataFileName).normalize();

        if(!metadataPath.startsWith(parent))
        {
            throw new RecordingSpoolException(
                    "Metadata path escapes the video directory"
            );
        }

        return metadataPath;
    }
    
    public List<Path> listMetadataFiles(Path directory)
    {
    	if(directory == null || directory.toString().isBlank())
    		throw new IllegalArgumentException("Directory cannot be null or blank");

    	Path normalizedDirectory =
    			directory.toAbsolutePath().normalize();

    	if(!Files.isDirectory(normalizedDirectory))
    		throw new IllegalArgumentException("Directory must exist and be a directory");

    	try(Stream<Path> paths = Files.list(normalizedDirectory))
    	{
    		return paths
    				.filter(Files::isRegularFile)
    				.filter(this::isMetadataFile)
    				.sorted()
    				.toList();
    	}
    	catch(IOException ex)
    	{
    		throw new RecordingSpoolException(
    				"Failed to list metadata files in directory: "+ normalizedDirectory,ex);
    	}
    }

    private void moveTemporaryMetadata(
            Path tempPath,
            Path metadataPath
    ) throws IOException
    {
        try
        {
            Files.move(
                    tempPath,
                    metadataPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        }
        catch(AtomicMoveNotSupportedException ex)
        {
            Files.move(
                    tempPath,
                    metadataPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private Path validateMetadataPath(Path metadataPath)
    {
        if(metadataPath == null
                || metadataPath.toString().isBlank())
        {
            throw new IllegalArgumentException(
                    "MetadataPath cannot be null or blank"
            );
        }

        Path normalizedMetadataPath =
                metadataPath.toAbsolutePath().normalize();

        Path fileNamePath =
                normalizedMetadataPath.getFileName();

        if(fileNamePath == null
                || !fileNamePath
                        .toString()
                        .toLowerCase()
                        .endsWith(".properties"))
        {
            throw new IllegalArgumentException(
                    "MetadataPath must be a .properties file"
            );
        }

        return normalizedMetadataPath;
    }

    private String getRequiredProperty(
            Properties properties,
            String key
    )
    {
        String value =
                properties.getProperty(key);

        if(value == null || value.isBlank())
        {
            throw new RecordingSpoolException(
                    "Required recording metadata property is missing: "
                            + key
            );
        }

        return value;
    }

    private boolean isMetadataFile(Path path)
    {
        Path fileName =
                path.getFileName();

        return fileName != null
                && fileName
                        .toString()
                        .toLowerCase()
                        .endsWith(".properties");
    }
    
    private Instant parseOptionalInstant(String value)
    {
    	if(value == null || value.isBlank())
    		return null;

    	return Instant.parse(value.trim());
    }

    private String normalizeOptionalProperty(String value)
    {
    	if(value == null)
    		return null;

    	String normalizedValue = value.trim();

    	if(normalizedValue.isEmpty())
    		return null;

    	return normalizedValue;
    }
}