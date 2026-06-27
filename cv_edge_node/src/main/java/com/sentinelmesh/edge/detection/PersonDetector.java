package com.sentinelmesh.edge.detection;

import java.util.List;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.yolo.YoloDetectionResultMapper;
import com.sentinelmesh.edge.yolo.YoloInput;
import com.sentinelmesh.edge.yolo.YoloPostProcessor;
import com.sentinelmesh.edge.yolo.YoloPrediction;
import com.sentinelmesh.edge.yolo.YoloPreprocessor;
import com.sentinelmesh.edge.yolo.v1.YoloV1Model;
import com.sentinelmesh.edge.yolo.v1.YoloV1Output;
import com.sentinelmesh.edge.yolo.v1.YoloV1OutputDecoder;
import com.sentinelmesh.edge.yolo.v1.YoloV1RawOutput;

public class PersonDetector implements Detector
{
	private final YoloPreprocessor preprocessor;
	private final YoloV1Model model;
	private final YoloV1OutputDecoder decoder;
	private final YoloPostProcessor postProcessor;
	private final YoloDetectionResultMapper mapper;

	public PersonDetector(
			YoloPreprocessor preprocessor,
			YoloV1Model model,
			YoloV1OutputDecoder decoder,
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
		
		YoloV1RawOutput rawOutput=model.forward(input);
		
		YoloV1Output decodedOutput=decoder.decode(rawOutput, input);
		
		YoloPrediction bestPrediction = null;

		for(YoloPrediction prediction : decodedOutput.getAllPredictions())
		{
		    if(bestPrediction == null || prediction.getScore() > bestPrediction.getScore())
		        bestPrediction = prediction;
		}

		if(bestPrediction != null)
		{
		    System.out.println(
		            "Best YOLO prediction: "
		            + "objectness=" + bestPrediction.getObjectness()
		            + ", classProbability=" + bestPrediction.getClassProbability()
		            + ", score=" + bestPrediction.getScore()
		            + ", box=(" + bestPrediction.getX()
		            + ", " + bestPrediction.getY()
		            + ", " + bestPrediction.getWidth()
		            + ", " + bestPrediction.getHeight() + ")"
		    );
		}
		
		List<YoloPrediction> predictions=postProcessor.process(decodedOutput);
		
		List<DetectionResult> results=mapper.toDetectionResults(predictions, frame);
		
		return List.copyOf(results);
	}
	
	public void close()
	{
		
	}
}