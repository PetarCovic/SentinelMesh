package com.sentinelmesh.edge.recording;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.sentinelmesh.edge.exceptions.RecordingSpoolException;

public class RecordingSpoolRecoveryService 
{
	private final RecordingSpoolDirectories spoolDirectories;
	private final RecordingSpoolMetadataStore metadataStore;
	private final RecordingSpoolService spoolService;
	
	public RecordingSpoolRecoveryService(
			RecordingSpoolDirectories spoolDirectories,
			RecordingSpoolMetadataStore metadataStore,
			RecordingSpoolService spoolService
			)
	{
		if(spoolDirectories==null)
			throw new IllegalArgumentException("SpoolDirectories cannot be null");
		
		if(metadataStore==null)
			throw new IllegalArgumentException("MetadataStore cannot be null");
		
		if(spoolService==null)
			throw new IllegalArgumentException("SpoolService cannot be null");
		
		this.spoolDirectories=spoolDirectories;
		this.metadataStore=metadataStore;
		this.spoolService=spoolService;
	}
	
	public void recover()
	{
		recoverActiveRecordings();
		recoverOrphanedActiveVideos();
		
		recoverPendingRecordings();
		recoverOrphanedPendingVideos();
		
		recoverUploadingRecordings();
		recoverOrphanedUploadingVideos();
		
		recoverUploadedRecordings();
		recoverOrphanedUploadedVideos();
		
		recoverFailedRecordings();
		recoverOrphanedFailedVideos();
	}
	
	private void recoverActiveRecordings()
	{
		List<Path> metadataFiles=metadataStore
				.listMetadataFiles(spoolDirectories.getActive());
		
		
		for(Path path : metadataFiles)
		{
			try
			{
				recoverActiveMetadataFile(path);
			}
			catch(Exception ex)
			{
				System.err.println("ActiveEntry recovery failed: "
						+ex.getLocalizedMessage());
			}
		}
	}
	
	private void recoverPendingRecordings()
	{
		List<Path> metadataFiles=metadataStore
				.listMetadataFiles(spoolDirectories.getPending());
		
		
		for(Path path : metadataFiles)
		{
			try
			{
				recoverPendingMetadataFile(path);
			}
			catch(Exception ex)
			{
				System.err.println("PendingEntry recovery failed: "
						+ex.getLocalizedMessage());
			}
		}
	}
	
	private void recoverUploadingRecordings()
	{
		List<Path> metadataFiles=metadataStore
				.listMetadataFiles(spoolDirectories.getUploading());
		
		
		for(Path path : metadataFiles)
		{
			try
			{
				recoverUploadingMetadataFile(path);
			}
			catch(Exception ex)
			{
				System.err.println("UploadingEntry recovery failed: "
						+ex.getLocalizedMessage());
			}
		}
	}
	
	private void recoverUploadedRecordings()
	{
		List<Path> metadataFiles=metadataStore
				.listMetadataFiles(spoolDirectories.getUploaded());
		
		
		for(Path path : metadataFiles)
		{
			try
			{
				recoverUploadedMetadataFile(path);
			}
			catch(Exception ex)
			{
				System.err.println("UploadedEntry recovery failed: "
						+ex.getLocalizedMessage());
			}
		}
	}
	
	private void recoverFailedRecordings()
	{
		List<Path> metadataFiles=metadataStore
				.listMetadataFiles(spoolDirectories.getFailed());
		
		
		for(Path path : metadataFiles)
		{
			try
			{
				recoverFailedMetadataFile(path);
			}
			catch(Exception ex)
			{
				System.err.println("FailedEntry recovery failed: "
						+ex.getLocalizedMessage());
			}
		}
	}
	
	private void recoverActiveRecording(RecordingSpoolEntry activeEntry)
	{
		if(activeEntry==null)
			throw new IllegalArgumentException("ActiveEntry cannot be null");
		
		if(activeEntry.getStatus()!=RecordingSpoolStatus.ACTIVE)
			throw new IllegalArgumentException("ActiveEntry status must be ACTIVE");
		
		if(Files.isRegularFile(activeEntry.getVideoPath()))
			spoolService.moveToFailed(activeEntry, "Recording was interrupted before the "
					+ "segment could be finalized");
		else if(!Files.exists(activeEntry.getVideoPath()))
			metadataStore.delete(metadataStore.getMetadataPath(activeEntry.getVideoPath()));
		else
			throw new RecordingSpoolException(
					"Active recording path exists but is not a regular file: "
							+ activeEntry.getVideoPath());
	}
	
	private void recoverPendingRecording(RecordingSpoolEntry pendingEntry)
	{
		if(pendingEntry==null)
			throw new IllegalArgumentException("PendingEntry cannot be null");
		
		if(pendingEntry.getStatus()!=RecordingSpoolStatus.PENDING)
			throw new IllegalArgumentException("PendingEntry status must be PENDING");
		
		if(Files.isRegularFile(pendingEntry.getVideoPath()))
			return;
		else if(!Files.exists(pendingEntry.getVideoPath()))
			metadataStore.delete(metadataStore.getMetadataPath(pendingEntry.getVideoPath()));
		else
			throw new RecordingSpoolException(
					"Pending recording path exists but is not a regular file: "
							+ pendingEntry.getVideoPath());
	}
	
	private void recoverUploadingRecording(RecordingSpoolEntry uploadingEntry)
	{
		if(uploadingEntry==null)
			throw new IllegalArgumentException("UploadingEntry cannot be null");
		
		if(uploadingEntry.getStatus()!=RecordingSpoolStatus.UPLOADING
				&& uploadingEntry.getStatus()!=RecordingSpoolStatus.UPLOAD_CONFIRMED)
			throw new IllegalArgumentException("UploadingEntry status must be UPLOADING "
					+ "or UPLOAD_CONFIRMED");
		
		if(Files.isRegularFile(uploadingEntry.getVideoPath()))
			return;
		else
			throw new RecordingSpoolException(
					"Uploading recording path exists but is not a regular file: "
							+ uploadingEntry.getVideoPath());
	}
	
	private void recoverUploadedRecording(RecordingSpoolEntry uploadedEntry)
	{
		if(uploadedEntry==null)
			throw new IllegalArgumentException("UploadedEntry cannot be null");
		
		if(uploadedEntry.getStatus()!=RecordingSpoolStatus.UPLOADED)
			throw new IllegalArgumentException("UploadedEntry status must be UPLOADED");
		
		if(Files.isRegularFile(uploadedEntry.getVideoPath()))
			return;
		else
			throw new RecordingSpoolException(
					"Uploaded recording path exists but is not a regular file: "
							+ uploadedEntry.getVideoPath());
	}
	
	private void recoverFailedRecording(RecordingSpoolEntry failedEntry)
	{
		if(failedEntry==null)
			throw new IllegalArgumentException("FailedEntry cannot be null");
		
		if(failedEntry.getStatus()!=RecordingSpoolStatus.FAILED)
			throw new IllegalArgumentException("FailedEntry status must be FAILED");
		
		if(Files.isRegularFile(failedEntry.getVideoPath()))
			return;
		else
			throw new RecordingSpoolException(
					"Failed recording path exists but is not a regular file: "
							+ failedEntry.getVideoPath());
	}
	
	private void recoverActiveMetadataFile(Path metadataPath)
	{
		RecordingSpoolEntry entry=null;
		
		try
		{
			entry=metadataStore.load(metadataPath);
		}
		catch(Exception ex)
		{
			handleMalformedMetadata(metadataPath, ex);
			return;
		}
		
		if(entry.getStatus() != RecordingSpoolStatus.ACTIVE)
		{
			handleInconsistentEntry(metadataPath, entry,
					"Metadata was found in active directory with status "+ entry.getStatus());
			
			return;
		}
		
		if(Files.exists(entry.getVideoPath()) && !Files.isRegularFile(entry.getVideoPath()))
		{
			handleInconsistentEntry(metadataPath, entry,
					"Video path exists but is not a regular file");

			return;
		}
		
		recoverActiveRecording(entry);
	}
	
	private void recoverPendingMetadataFile(Path metadataPath)
	{
		RecordingSpoolEntry entry;
		
		try
		{
			entry=metadataStore.load(metadataPath);
		}
		catch(Exception ex)
		{
			handleMalformedMetadata(metadataPath, ex);
			return;
		}
		
		if(entry.getStatus() != RecordingSpoolStatus.PENDING)
		{
			handleInconsistentEntry(metadataPath, entry,
					"Metadata was found in pending directory with status "+ entry.getStatus());
			
			return;
		}
		
		if(Files.exists(entry.getVideoPath()) && !Files.isRegularFile(entry.getVideoPath()))
		{
			handleInconsistentEntry(metadataPath, entry,
					"Video path exists but is not a regular file");

			return;
		}
		
		recoverPendingRecording(entry);
	}
	
	private void recoverUploadingMetadataFile(Path metadataPath)
	{
		RecordingSpoolEntry entry;
		
		try
		{
			entry=metadataStore.load(metadataPath);
		}
		catch(Exception ex)
		{
			handleMalformedMetadata(metadataPath, ex);
			return;
		}
		
		RecordingSpoolStatus status = entry.getStatus();

		if(status != RecordingSpoolStatus.UPLOADING
				&& status != RecordingSpoolStatus.UPLOAD_CONFIRMED)
		{
			handleInconsistentEntry(
					metadataPath,
					entry,
					"Metadata was found in uploading directory with status " + status
			);

			return;
		}
		
		if(!Files.exists(entry.getVideoPath()))
		{
			if(status == RecordingSpoolStatus.UPLOADING)
			{
				handleInconsistentEntry(
						metadataPath,
						entry,
						"Uploading recording video file is missing"
				);
			}
			else if(status == RecordingSpoolStatus.UPLOAD_CONFIRMED)
			{
				metadataStore.delete(metadataPath);
			}

			return;
		}
		
		if(Files.exists(entry.getVideoPath()) && !Files.isRegularFile(entry.getVideoPath()))
		{
			handleInconsistentEntry(metadataPath, entry,
					"Video path exists but is not a regular file");

			return;
		}
		
		recoverUploadingRecording(entry);
	}
	
	private void recoverUploadedMetadataFile(Path metadataPath)
	{
		RecordingSpoolEntry entry;
		
		try
		{
			entry=metadataStore.load(metadataPath);
		}
		catch(Exception ex)
		{
			handleMalformedMetadata(metadataPath, ex);
			return;
		}
		
		RecordingSpoolStatus status = entry.getStatus();

		if(status != RecordingSpoolStatus.UPLOADED)
		{
			handleInconsistentEntry(
					metadataPath,
					entry,
					"Metadata was found in uploaded directory with status " + status
			);

			return;
		}
		
		if(!Files.exists(entry.getVideoPath()))
		{
			metadataStore.delete(metadataPath);
			return;
		}
		
		if(Files.exists(entry.getVideoPath()) && !Files.isRegularFile(entry.getVideoPath()))
		{
			handleInconsistentEntry(metadataPath, entry,
					"Video path exists but is not a regular file");

			return;
		}
		
		recoverUploadedRecording(entry);
	}
	
	private void recoverFailedMetadataFile(Path metadataPath)
	{
		RecordingSpoolEntry entry;
		
		try
		{
			entry=metadataStore.load(metadataPath);
		}
		catch(Exception ex)
		{
			System.err.println("Failed to recover metadata: "+metadataPath);
			return;
		}
		
		RecordingSpoolStatus status = entry.getStatus();

		if(status != RecordingSpoolStatus.FAILED)
		{
			System.err.println("Metadata was found in failed directory with status " + status);

			return;
		}
		
		if(!Files.exists(entry.getVideoPath()))
		{
			System.err.println("File does not exist for "+entry.getVideoPath());
			return;
		}
		
		if(Files.exists(entry.getVideoPath()) && !Files.isRegularFile(entry.getVideoPath()))
		{
			System.err.println("Failed recording video path is not a regular file: "
					+ entry.getVideoPath());

			return;
		}
		
		recoverFailedRecording(entry);
	}
	
	private void recoverOrphanedActiveVideos()
	{
		Path activeDirectory =spoolDirectories.getActive().toAbsolutePath().normalize();

		Path failedDirectory =spoolDirectories.getFailed().toAbsolutePath().normalize();

		try(Stream<Path> paths = Files.list(activeDirectory))
		{
			paths
				.filter(Files::isRegularFile)
				.filter(path ->
						path.getFileName()
								.toString()
								.toLowerCase()
								.endsWith(".mp4"))
				.filter(videoPath ->
						!Files.exists(
								metadataStore.getMetadataPath(videoPath)
						))
				.forEach(videoPath ->
				{
					try
					{
						String originalFilename =videoPath.getFileName().toString();

						Path failedPath =failedDirectory.resolve(originalFilename);

						if(Files.exists(failedPath))
						{
							failedPath = failedDirectory.resolve("orphaned-"
											+ UUID.randomUUID()
											+ "-"
											+ originalFilename);
						}

						try
						{
							Files.move(videoPath, failedPath, StandardCopyOption.ATOMIC_MOVE);
						}
						catch(AtomicMoveNotSupportedException ex)
						{
							Files.move(videoPath, failedPath);
						}

						System.err.println("Moved orphaned active recording to failed: "
										+ failedPath);
					}
					catch(IOException ex)
					{
						System.err.println("Failed to recover orphaned recording "
										+ videoPath+ ": "+ ex.getLocalizedMessage());
					}
				});
		}
		catch(IOException ex)
		{
			throw new RecordingSpoolException("Failed to scan active directory for "
							+ "orphaned recordings: "+ activeDirectory, ex);
		}
	}
	
	private void recoverOrphanedPendingVideos()
	{
		Path pendingDirectory =spoolDirectories.getPending().toAbsolutePath().normalize();

		Path failedDirectory =spoolDirectories.getFailed().toAbsolutePath().normalize();

		try(Stream<Path> paths = Files.list(pendingDirectory))
		{
			paths
				.filter(Files::isRegularFile)
				.filter(path ->
						path.getFileName()
								.toString()
								.toLowerCase()
								.endsWith(".mp4"))
				.filter(videoPath ->
						!Files.exists(
								metadataStore.getMetadataPath(videoPath)
						))
				.forEach(videoPath ->
				{
					try
					{
						String originalFilename =videoPath.getFileName().toString();

						Path failedPath =failedDirectory.resolve(originalFilename);

						if(Files.exists(failedPath))
						{
							failedPath = failedDirectory.resolve("orphaned-"
											+ UUID.randomUUID()
											+ "-"
											+ originalFilename);
						}

						try
						{
							Files.move(videoPath, failedPath, StandardCopyOption.ATOMIC_MOVE);
						}
						catch(AtomicMoveNotSupportedException ex)
						{
							Files.move(videoPath, failedPath);
						}

						System.err.println("Moved orphaned pending recording to failed: "
										+ failedPath);
					}
					catch(IOException ex)
					{
						System.err.println("Failed to recover orphaned recording "
										+ videoPath+ ": "+ ex.getLocalizedMessage());
					}
				});
		}
		catch(IOException ex)
		{
			throw new RecordingSpoolException("Failed to scan pending directory for "
							+ "orphaned recordings: "+ pendingDirectory, ex);
		}
	}
	
	private void recoverOrphanedUploadingVideos()
	{
		Path uploadingDirectory =spoolDirectories.getUploading().toAbsolutePath().normalize();

		Path failedDirectory =spoolDirectories.getFailed().toAbsolutePath().normalize();

		try(Stream<Path> paths = Files.list(uploadingDirectory))
		{
			paths
				.filter(Files::isRegularFile)
				.filter(path ->
						path.getFileName()
								.toString()
								.toLowerCase()
								.endsWith(".mp4"))
				.filter(videoPath ->
						!Files.exists(
								metadataStore.getMetadataPath(videoPath)
						))
				.forEach(videoPath ->
				{
					try
					{
						String originalFilename =videoPath.getFileName().toString();

						Path failedPath =failedDirectory.resolve(originalFilename);

						if(Files.exists(failedPath))
						{
							failedPath = failedDirectory.resolve("orphaned-"
											+ UUID.randomUUID()
											+ "-"
											+ originalFilename);
						}

						try
						{
							Files.move(videoPath, failedPath, StandardCopyOption.ATOMIC_MOVE);
						}
						catch(AtomicMoveNotSupportedException ex)
						{
							Files.move(videoPath, failedPath);
						}

						System.err.println("Moved orphaned uploading recording to failed: "
										+ failedPath);
					}
					catch(IOException ex)
					{
						System.err.println("Failed to recover orphaned recording "
										+ videoPath+ ": "+ ex.getLocalizedMessage());
					}
				});
		}
		catch(IOException ex)
		{
			throw new RecordingSpoolException("Failed to scan uploading directory for "
							+ "orphaned recordings: "+ uploadingDirectory, ex);
		}
	}
	
	private void recoverOrphanedUploadedVideos()
	{
		Path uploadedDirectory =spoolDirectories.getUploaded().toAbsolutePath().normalize();

		Path failedDirectory =spoolDirectories.getFailed().toAbsolutePath().normalize();

		try(Stream<Path> paths = Files.list(uploadedDirectory))
		{
			paths
				.filter(Files::isRegularFile)
				.filter(path ->
						path.getFileName()
								.toString()
								.toLowerCase()
								.endsWith(".mp4"))
				.filter(videoPath ->
						!Files.exists(
								metadataStore.getMetadataPath(videoPath)
						))
				.forEach(videoPath ->
				{
					try
					{
						String originalFilename =videoPath.getFileName().toString();

						Path failedPath =failedDirectory.resolve(originalFilename);

						if(Files.exists(failedPath))
						{
							failedPath = failedDirectory.resolve("orphaned-"
											+ UUID.randomUUID()
											+ "-"
											+ originalFilename);
						}

						try
						{
							Files.move(videoPath, failedPath, StandardCopyOption.ATOMIC_MOVE);
						}
						catch(AtomicMoveNotSupportedException ex)
						{
							Files.move(videoPath, failedPath);
						}

						System.err.println("Moved orphaned uploaded recording to failed: "
										+ failedPath);
					}
					catch(IOException ex)
					{
						System.err.println("Failed to recover orphaned recording "
										+ videoPath+ ": "+ ex.getLocalizedMessage());
					}
				});
		}
		catch(IOException ex)
		{
			throw new RecordingSpoolException("Failed to scan uploaded directory for "
							+ "orphaned recordings: "+ uploadedDirectory, ex);
		}
	}
	
	private void recoverOrphanedFailedVideos()
	{
		Path failedDirectory = spoolDirectories.getFailed().toAbsolutePath().normalize();

		try(Stream<Path> paths = Files.list(failedDirectory))
		{
			paths
				.filter(Files::isRegularFile)
				.filter(path ->
						path.getFileName()
								.toString()
								.toLowerCase()
								.endsWith(".mp4"))
				.filter(videoPath ->
						!Files.exists(metadataStore.getMetadataPath(videoPath)
						))
				.forEach(videoPath ->
				{
					System.err.println("Found orphaned failed recording: " + videoPath);
				});
		}
		catch(IOException ex)
		{
			throw new RecordingSpoolException(
					"Failed to scan failed directory for orphaned recordings: "
							+ failedDirectory, ex);
		}
	}
	
	private void handleMalformedMetadata(Path metadataPath, Exception failure)
	{
		if(metadataPath == null)
			throw new IllegalArgumentException("MetadataPath cannot be null");

		if(failure == null)
			throw new IllegalArgumentException("Failure cannot be null");

		Path normalizedMetadataPath=metadataPath.toAbsolutePath().normalize();

		Path failedDirectory=spoolDirectories.getFailed().toAbsolutePath().normalize();

		String originalFilename=normalizedMetadataPath.getFileName().toString();

		Path failedMetadataPath =
				failedDirectory.resolve("malformed-"+ UUID.randomUUID()+ "-"+ originalFilename);

		try
		{
			Files.createDirectories(failedDirectory);

			try
			{
				Files.move(
						normalizedMetadataPath,
						failedMetadataPath, 
						StandardCopyOption.ATOMIC_MOVE);
			}
			catch(AtomicMoveNotSupportedException ex)
			{
				Files.move(normalizedMetadataPath,failedMetadataPath);
			}

			System.err.println(
					"Moved malformed metadata to failed: "
							+ failedMetadataPath+ ". Cause: "+ failure.getLocalizedMessage());
		}
		catch(IOException ex)
		{
			ex.addSuppressed(failure);

			throw new RecordingSpoolException(
					"Failed to move malformed metadata to failed: "+ normalizedMetadataPath,ex);
		}
	}
	
	private void handleInconsistentEntry(
			Path metadataPath,
			RecordingSpoolEntry entry,
			String reason)
	{
		if(metadataPath == null)
			throw new IllegalArgumentException("MetadataPath cannot be null");

		if(entry == null)
			throw new IllegalArgumentException("Entry cannot be null");

		if(reason == null || reason.isBlank())
			throw new IllegalArgumentException("Reason cannot be null or blank");

		Path normalizedMetadataPath =metadataPath.toAbsolutePath().normalize();

		Path videoPath =entry.getVideoPath().toAbsolutePath().normalize();

		Path failedDirectory =spoolDirectories.getFailed().toAbsolutePath().normalize();

		Path metadataFilename =normalizedMetadataPath.getFileName();

		if(metadataFilename == null)
			throw new RecordingSpoolException("Metadata path does not contain a filename: "
							+ normalizedMetadataPath);

		String recoveryId = UUID.randomUUID().toString();

		Path failedMetadataPath =failedDirectory.resolve("inconsistent-"+ recoveryId+ "-"
								+ metadataFilename);

		Path videoFilename = videoPath.getFileName();

		Path failedVideoPath = null;

		if(videoFilename != null)
		{
			failedVideoPath =failedDirectory.resolve("inconsistent-"
									+ recoveryId
									+ "-"
									+ videoFilename);
		}

		try
		{
			Files.createDirectories(failedDirectory);

			moveWithAtomicFallback(normalizedMetadataPath,failedMetadataPath);

			if(Files.isRegularFile(videoPath) && failedVideoPath != null)
			{
				moveWithAtomicFallback(videoPath,failedVideoPath);
			}
			else if(Files.exists(videoPath))
			{
				System.err.println("Inconsistent recording video path was not moved "
								+ "because it is not a regular file: "
								+ videoPath);
			}

			System.err.println("Moved inconsistent recording metadata to failed. "
							+ "Segment: "
							+ entry.getSegmentId()
							+ ". Reason: "
							+ reason);
		}
		catch(IOException ex)
		{
			throw new RecordingSpoolException(
					"Failed to quarantine inconsistent recording "+ entry.getSegmentId(), ex);
		}
	}
	
	private void moveWithAtomicFallback(Path source,Path destination)throws IOException
	{
		try
		{
			Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
		}
		catch(AtomicMoveNotSupportedException ex)
		{
			Files.move(source, destination);
		}
	}
}