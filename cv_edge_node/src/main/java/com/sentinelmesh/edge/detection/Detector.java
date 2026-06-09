package com.sentinelmesh.edge.detection;

import java.util.List;

import com.sentinelmesh.edge.camera.Frame;

public interface Detector 
{
	List<DetectionResult> detect(Frame frame);
}
