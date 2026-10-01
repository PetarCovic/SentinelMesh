package com.sentinelmesh.edge.media;

import java.util.List;
import java.util.Locale;

import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.detection.DetectionResult;

public class FrameAnnotationRenderer
{
	public Frame renderCopy(Frame frame, List<DetectionResult> detections)
	{
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");

		if(detections==null)
			throw new IllegalArgumentException("Detections cannot be null");

		Frame copy=null;

		try
		{
			copy=frame.copy();
			if(!detections.isEmpty())
			    throw new IllegalStateException("Temporary annotation failure test");
			for(DetectionResult result : detections)
			{
				if(result==null)
					continue;

				if(!result.hasBoundingBox())
					continue;

				drawDetection(copy.getImage(), result);
			}
		}
		catch(RuntimeException ex)
		{
			if (copy!=null)
		    {
		        try
		        {
		            copy.close();
		        }
		        catch (RuntimeException cleanupEx)
		        {
		            ex.addSuppressed(cleanupEx);
		        }
		    }

		    throw ex;
		}

		return copy;
	}

	private void drawDetection(Mat image, DetectionResult result)
	{
		try (
		        Rect box = new Rect(
		            result.getBoundingBoxX(),
		            result.getBoundingBoxY(),
		            result.getBoundingBoxWidth(),
		            result.getBoundingBoxHeight()
		        );

		        Scalar red = new Scalar(0, 0, 255, 0);

		        Point org=new Point(
                    result.getBoundingBoxX(),
                    Math.max(20, result.getBoundingBoxY()-5)
		        );
		    )
		    {
		        opencv_imgproc.rectangle(
		            image,
		            box,
		            red,
		            2,
		            opencv_imgproc.LINE_8,
		            0
		        );

		        opencv_imgproc.putText(
                        image,
                        formatLabel(result),
                        org,
                        opencv_imgproc.FONT_HERSHEY_SIMPLEX,
                        .5,
                        red,
                        1,
                        opencv_imgproc.LINE_AA,
                        false
                        );
		    }
	}

	private String formatLabel(DetectionResult detection)
	{
		return String.format(
	            Locale.ROOT,
	            "%s %.1f%%",
	            detection.getType(),
	            detection.getConfidence() * 100
	    );
	}
}