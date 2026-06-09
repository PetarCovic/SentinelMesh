package com.sentinelmesh.edge.debug;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.detection.DetectionResult;

public class FrameDebugViewer 
{
	private final JFrame window;
	private final JLabel imageLabel;
	private volatile boolean open;
	
	public FrameDebugViewer(String title)
	{
		this.window = new JFrame(title);
		this.imageLabel = new JLabel();
		this.open = true;
		
		window.setLayout(new BorderLayout());
		window.add(imageLabel, BorderLayout.CENTER);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.setSize(800, 600);
		window.setLocationRelativeTo(null);
		window.setVisible(true);
		
		window.addWindowListener(new java.awt.event.WindowAdapter() 
		{
			@Override
			public void windowClosed(java.awt.event.WindowEvent e) 
			{
				open = false;
			}
		});
	}
	
	public boolean isOpen()
	{
		return open;
	}
	
	public void show(Frame frame, List<DetectionResult> detections)
	{
		if(frame == null)
			throw new IllegalArgumentException("Frame cannot be null");
		
		if(frame.isEmpty())
			return;
		
		if(!open)
			return;
		
		BufferedImage image = matToBufferedImage(frame.getImage());
		
		if(detections != null && !detections.isEmpty())
		{
			drawDetections(image, detections);
		}
		
		SwingUtilities.invokeLater(() -> {
			imageLabel.setIcon(new ImageIcon(image));
			window.pack();
		});
	}
	
	private void drawDetections(BufferedImage image, List<DetectionResult> detections)
	{
		Graphics2D g = image.createGraphics();
		
		try
		{
			g.setColor(Color.RED);
			g.setStroke(new BasicStroke(3));
			
			for(DetectionResult detection : detections)
			{
				if(detection == null || !detection.hasBoundingBox())
					continue;
				
				g.drawRect(
						detection.getBoundingBoxX(),
						detection.getBoundingBoxY(),
						detection.getBoundingBoxWidth(),
						detection.getBoundingBoxHeight()
				);
				
				String label = detection.getType() + " " 
						+ String.format("%.2f", detection.getConfidence());
				
				g.drawString(
						label,
						detection.getBoundingBoxX(),
						Math.max(15, detection.getBoundingBoxY() - 5)
				);
			}
		}
		finally
		{
			g.dispose();
		}
	}
	
	private BufferedImage matToBufferedImage(Mat mat)
	{
		if(mat == null || mat.empty())
			throw new IllegalArgumentException("Mat cannot be null or empty");
		
		Mat converted = new Mat();
		
		try
		{
			int channels = mat.channels();
			
			if(channels == 1)
			{
				opencv_imgproc.cvtColor(mat, converted, opencv_imgproc.COLOR_GRAY2RGB);
			}
			else if(channels == 3)
			{
				opencv_imgproc.cvtColor(mat, converted, opencv_imgproc.COLOR_BGR2RGB);
			}
			else if(channels == 4)
			{
				opencv_imgproc.cvtColor(mat, converted, opencv_imgproc.COLOR_BGRA2RGB);
			}
			else
			{
				throw new IllegalArgumentException("Unsupported Mat channel count: " + channels);
			}
			
			int width = converted.cols();
			int height = converted.rows();
			int type = BufferedImage.TYPE_3BYTE_BGR;
			
			BufferedImage image = new BufferedImage(width, height, type);
			
			byte[] data = new byte[width * height * 3];
			converted.data().get(data);
			
			image.getRaster().setDataElements(0, 0, width, height, data);
			
			return image;
		}
		finally
		{
			converted.release();
		}
	}
	
	public void close()
	{
		open = false;
		
		SwingUtilities.invokeLater(() -> {
			if(window != null)
			{
				window.dispose();
			}
		});
	}
}