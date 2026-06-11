package com.sentinelmesh.edge.detection;

import java.util.List;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.yolo.YoloDetectionResultMapper;
import com.sentinelmesh.edge.yolo.YoloInput;
import com.sentinelmesh.edge.yolo.YoloModel;
import com.sentinelmesh.edge.yolo.YoloOutput;
import com.sentinelmesh.edge.yolo.YoloOutputDecoder;
import com.sentinelmesh.edge.yolo.YoloPostProcessor;
import com.sentinelmesh.edge.yolo.YoloPrediction;
import com.sentinelmesh.edge.yolo.YoloPreprocessor;
import com.sentinelmesh.edge.yolo.YoloRawOutput;

public class PersonDetector implements Detector
{
	private final YoloPreprocessor preprocessor;
	private final YoloModel model;
	private final YoloOutputDecoder decoder;
	private final YoloPostProcessor postProcessor;
	private final YoloDetectionResultMapper mapper;

	public PersonDetector(
			YoloPreprocessor preprocessor,
			YoloModel model,
			YoloOutputDecoder decoder,
			YoloPostProcessor postProcessor,
			YoloDetectionResultMapper mapper
			)
	{
		if(preprocessor==null)
			throw new IllegalArgumentException("Preprocessor cannot be null");

		if(model==null)
			throw new IllegalArgumentException("Model cannot be null");
		
		if(decoder==null)
			throw new IllegalArgumentException("Decoder cannot be null");
		
		if(postProcessor==null)
			throw new IllegalArgumentException("PostProcessor cannot be null");
		
		if(mapper==null)
			throw new IllegalArgumentException("Mapper cannot be null");
		
		this.preprocessor=preprocessor;
		this.model=model;
		this.decoder=decoder;
		this.postProcessor=postProcessor;
		this.mapper=mapper;
	}
	
	@Override
	public List<DetectionResult> detect(Frame frame)
	{
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");
		
		YoloInput input=preprocessor.preprocess(frame);
		
		YoloRawOutput rawOutput=model.forward(input);
		
		YoloOutput decodedOutput=decoder.decode(rawOutput, input);
		
		List<YoloPrediction> predictions=postProcessor.process(decodedOutput);
		
		List<DetectionResult> results=mapper.toDetectionResults(predictions, frame);
		
		return List.copyOf(results);
	}
	
	public void close()
	{
		
	}
}