package com.sentinelmesh.live;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class LiveFrameController
{
	private static final byte[] MJPEG_FRAME_END="\r\n".getBytes(StandardCharsets.US_ASCII);
	
	private final LiveFrameService liveFrameService;
	
	public LiveFrameController(LiveFrameService liveFrameService)
	{
		if(liveFrameService==null)
			throw new IllegalArgumentException("LiveFrameService cannot be null");
		
		this.liveFrameService = liveFrameService;
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping(
			value="/devices/{deviceId}/live-frame",
			consumes=MediaType.MULTIPART_FORM_DATA_VALUE
			)
	public void storeFrame(@PathVariable UUID deviceId, @RequestPart("file") MultipartFile file)
	{
		liveFrameService.storeFrame(deviceId, file);
	}
	
	@GetMapping(value="/devices/{deviceId}/live")
	public StreamingResponseBody streamLive(
			@PathVariable UUID deviceId,
			HttpServletResponse response)
	{
		liveFrameService.validateCamera(deviceId);
		
		response.setContentType(
				"multipart/x-mixed-replace; boundary=frame"
				);
		
		return outputStream ->
		{
			Instant lastSentTimestamp = null;
			
			while(true)
			{
				Optional<LiveFrameData> optionalFrame =
						liveFrameService.getLatestFrame(deviceId);
				
				if(optionalFrame.isEmpty())
				{
					try
					{
						Thread.sleep(50);
						continue;
					}
					catch(InterruptedException ex)
					{
						Thread.currentThread().interrupt();
						break;
					}
				}
				
				LiveFrameData frame = optionalFrame.get();
				
				if(lastSentTimestamp != null
						&& frame.getReceivedAt().equals(lastSentTimestamp))
				{
					try
					{
						Thread.sleep(50);
						continue;
					}
					catch(InterruptedException ex)
					{
						Thread.currentThread().interrupt();
						break;
					}
				}
				
				int jpegSize = frame.getJpegSize();

				StringBuilder headerBuilder = new StringBuilder();

				headerBuilder.append("--frame\r\n");
				headerBuilder.append("Content-Type: image/jpeg\r\n");
				headerBuilder.append("Content-Length: ")
							 .append(jpegSize)
							 .append("\r\n");
				headerBuilder.append("\r\n");
				
				try
				{
					outputStream.write(
							headerBuilder.toString()
									.getBytes(StandardCharsets.US_ASCII)
							);
					
					frame.writeJpegTo(outputStream);
					outputStream.write(MJPEG_FRAME_END);
					outputStream.flush();
				}
				catch(IOException ex)
				{
					break;
				}
				
				lastSentTimestamp = frame.getReceivedAt();
			}
		};
	}
}