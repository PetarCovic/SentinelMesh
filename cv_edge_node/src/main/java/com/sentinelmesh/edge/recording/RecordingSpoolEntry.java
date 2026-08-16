package com.sentinelmesh.edge.recording;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public class RecordingSpoolEntry 
{
	private final UUID segmentId;
	private final UUID deviceId;
	private final Path videoPath;
	private final Instant segmentStartTime;
	private final Instant segmentEndTime;
	private final long fileSizeBytes;
	private final int uploadAttempts;
	private final RecordingSpoolStatus status;
	private final Instant createdAt;
	private final String lastUploadError;
	
	private final int cleanupAttempts;
	private final Instant nextCleanupAttemptAt;
	private final String lastCleanupError;
	
	public RecordingSpoolEntry(
			UUID segmentId,
			UUID deviceId,
			Path videoPath,
			Instant segmentStartTime,
			Instant segmentEndTime,
			long fileSizeBytes,
			int uploadAttempts,
			RecordingSpoolStatus status,
			Instant createdAt,
			String lastUploadError
			)
	{
		this(
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
			0,
			null,
			null
			);
	}
	
	public RecordingSpoolEntry(
			UUID segmentId,
			UUID deviceId,
			Path videoPath,
			Instant segmentStartTime,
			Instant segmentEndTime,
			long fileSizeBytes,
			int uploadAttempts,
			RecordingSpoolStatus status,
			Instant createdAt,
			String lastUploadError,
			int cleanupAttempts,
			Instant nextCleanupAttemptAt,
			String lastCleanupError
			)
	{
		if(segmentId==null)
			throw new IllegalArgumentException("SegmentId cannot be null");
		
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(videoPath==null || videoPath.toString().isBlank())
			throw new IllegalArgumentException("VideoPath cannot be null or blank");
		
		if(segmentStartTime==null)
			throw new IllegalArgumentException("SegmentStartTime cannot be null");
		
		if(segmentEndTime==null)
			throw new IllegalArgumentException("SegmentEndTime cannot be null");
		
		if(!segmentEndTime.isAfter(segmentStartTime))
			throw new IllegalArgumentException("SegmentEndTime must be after SegmentStartTime");
		
		if(fileSizeBytes<0)
			throw new IllegalArgumentException("FileSizeBytes cannot be negative");
		
		if(uploadAttempts<0)
			throw new IllegalArgumentException("UploadAttempts cannot be negative");
		
		if(status==null)
			throw new IllegalArgumentException("status cannot be null");
		
		if(createdAt==null)
			throw new IllegalArgumentException("CreatedAt cannot be null");
		
		if(cleanupAttempts < 0)
			throw new IllegalArgumentException("CleanupAttempts cannot be negative");
		
		this.segmentId=segmentId;
		this.deviceId=deviceId;
		this.videoPath=videoPath;
		this.segmentStartTime=segmentStartTime;
		this.segmentEndTime=segmentEndTime;
		this.fileSizeBytes=fileSizeBytes;
		this.uploadAttempts=uploadAttempts;
		this.status=status;
		this.createdAt=createdAt;
		this.lastUploadError=normalizeErrorMessage(lastUploadError);
		this.cleanupAttempts=cleanupAttempts;
		this.nextCleanupAttemptAt=nextCleanupAttemptAt;
		this.lastCleanupError=normalizeErrorMessage(lastCleanupError);
	}

	public UUID getSegmentId() {
		return segmentId;
	}

	public UUID getDeviceId() 
	{
		return deviceId;
	}

	public Path getVideoPath() 
	{
		return videoPath;
	}

	public Instant getSegmentStartTime() 
	{
		return segmentStartTime;
	}

	public Instant getSegmentEndTime() 
	{
		return segmentEndTime;
	}

	public long getFileSizeBytes()
	{
		return fileSizeBytes;
	}

	public int getUploadAttempts() 
	{
		return uploadAttempts;
	}

	public RecordingSpoolStatus getStatus() 
	{
		return status;
	}

	public Instant getCreatedAt() 
	{
		return createdAt;
	}

	public String getLastUploadError() 
	{
		return lastUploadError;
	}
	
	public int getCleanupAttempts()
	{
		return cleanupAttempts;
	}

	public Instant getNextCleanupAttemptAt()
	{
		return nextCleanupAttemptAt;
	}

	public String getLastCleanupError()
	{
		return lastCleanupError;
	}
	
	public RecordingSpoolEntry markUploading(Path uploadingPath)
	{
		return new RecordingSpoolEntry(
				segmentId,
				deviceId,
				uploadingPath,
				segmentStartTime,
				segmentEndTime,
				fileSizeBytes,
				uploadAttempts,
				RecordingSpoolStatus.UPLOADING,
				createdAt,
				lastUploadError
				);
	}
	
	public RecordingSpoolEntry markUploaded(Path uploadedPath)
	{
		return new RecordingSpoolEntry(
				segmentId,
				deviceId,
				uploadedPath,
				segmentStartTime,
				segmentEndTime,
				fileSizeBytes,
				uploadAttempts,
				RecordingSpoolStatus.UPLOADED,
				createdAt,
				null,
				0,
				null,
				null
				);
	}
	
	public RecordingSpoolEntry markUploadConfirmed()
	{
	    return new RecordingSpoolEntry(
	            segmentId,
	            deviceId,
	            videoPath,
	            segmentStartTime,
	            segmentEndTime,
	            fileSizeBytes,
	            uploadAttempts,
	            RecordingSpoolStatus.UPLOAD_CONFIRMED,
	            createdAt,
	            null,
	            0,
	            null,
	            null
	    );
	}
	
	public RecordingSpoolEntry recordUploadFailure(Path pendingPath, String error)
	{
		return new RecordingSpoolEntry(
				segmentId,
				deviceId,
				pendingPath,
				segmentStartTime,
				segmentEndTime,
				fileSizeBytes,
				uploadAttempts+1,
				RecordingSpoolStatus.PENDING,
				createdAt,
				error
				);
	}
	
	public RecordingSpoolEntry markPending(Path pendingPath)
	{
		return new RecordingSpoolEntry(
				segmentId,
				deviceId,
				pendingPath,
				segmentStartTime,
				segmentEndTime,
				fileSizeBytes,
				uploadAttempts,
				RecordingSpoolStatus.PENDING,
				createdAt,
				lastUploadError
				);
	}
	
	public RecordingSpoolEntry markFailed(Path failedPath, String error)
	{
		return new RecordingSpoolEntry(
				segmentId,
				deviceId,
				failedPath,
				segmentStartTime,
				segmentEndTime,
				fileSizeBytes,
				uploadAttempts,
				RecordingSpoolStatus.FAILED,
				createdAt,
				error
				);
	}
	
	public RecordingSpoolEntry recordPermanentUploadFailure(Path failedPath, String error)
	{
		return new RecordingSpoolEntry(
				segmentId,
				deviceId,
				failedPath,
				segmentStartTime,
				segmentEndTime,
				fileSizeBytes,
				uploadAttempts+1,
				RecordingSpoolStatus.FAILED,
				createdAt,
				error
				);
	}
	
	public RecordingSpoolEntry recordCleanupFailure(
			String cleanupError, 
			Instant nextAttemptAt)
	{
		if(status != RecordingSpoolStatus.UPLOAD_CONFIRMED)
			throw new IllegalStateException(
					"Only UPLOAD_CONFIRMED entries can record cleanup failures");

		if(nextAttemptAt == null)
			throw new IllegalArgumentException("NextAttemptAt cannot be null");

		if(!nextAttemptAt.isAfter(Instant.now()))
			throw new IllegalArgumentException("NextAttemptAt must be in the future");

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
				cleanupAttempts + 1,
				nextAttemptAt,
				requireErrorMessage(cleanupError)
		);
	}
	
	public boolean isCleanupRetryReady(Instant currentTime)
	{
		if(currentTime == null)
			throw new IllegalArgumentException("CurrentTime cannot be null");

		if(status != RecordingSpoolStatus.UPLOAD_CONFIRMED)
			return false;

		return nextCleanupAttemptAt == null || !nextCleanupAttemptAt.isAfter(currentTime);
	}
	
	private static String requireErrorMessage(String errorMessage)
	{
		String normalizedError =
				normalizeErrorMessage(errorMessage);

		if(normalizedError == null)
			throw new IllegalArgumentException(
					"Error message cannot be null or blank"
			);

		return normalizedError;
	}

	private static String normalizeErrorMessage(String errorMessage)
	{
		if(errorMessage == null)
			return null;

		String normalizedError = errorMessage.trim();

		if(normalizedError.isEmpty())
			return null;

		return normalizedError;
	}
}