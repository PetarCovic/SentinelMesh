package com.sentinelmesh.edge.yolo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.cnn.Activation1DLayer;
import com.sentinelmesh.edge.cnn.FlattenLayer;
import com.sentinelmesh.edge.cnn.FullyConnectedLayer;
import com.sentinelmesh.edge.cnn.Layer;
import com.sentinelmesh.edge.cnn.Tensor3D;
import com.sentinelmesh.edge.detection.DetectionResult;
import com.sentinelmesh.edge.detection.DetectionType;

public class YoloSanityTest 
{
	public static void main(String[] args)
	{
		testYoloInput();
		testYoloBoundingBox();
		testYoloPrediction();
		testYoloGridCell();
		testYoloOutput();
		testYoloRawOutput();
		testYoloDecoder();
		testYoloPostProcessor();
		testYoloModel();

		System.out.println();
		System.out.println("All YOLO sanity tests completed.");
	}

	private static void testYoloInput()
	{
		System.out.println("=== YoloInput test ===");

		float[][][] imageData = new float[2][2][3];

		imageData[0][0][0] = 1.0f;
		imageData[0][0][1] = 0.5f;
		imageData[0][0][2] = 0.25f;

		YoloInput input = new YoloInput(
				imageData,
				640,
				480,
				2,
				2
		);

		assertIntEquals(640, input.getOriginalWidth(), "originalWidth");
		assertIntEquals(480, input.getOriginalHeight(), "originalHeight");
		assertIntEquals(2, input.getInputWidth(), "inputWidth");
		assertIntEquals(2, input.getInputHeight(), "inputHeight");
		assertFloatEquals(1.0f, input.getImageData()[0][0][0], "imageData[0][0][0]");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testYoloBoundingBox()
	{
		System.out.println("=== YoloBoundingBox test ===");

		YoloBoundingBox box = new YoloBoundingBox(10.0, 20.0, 30.0, 40.0);

		assertDoubleEquals(10.0, box.getX(), "box.x");
		assertDoubleEquals(20.0, box.getY(), "box.y");
		assertDoubleEquals(30.0, box.getWidth(), "box.width");
		assertDoubleEquals(40.0, box.getHeight(), "box.height");
		assertDoubleEquals(40.0, box.getRight(), "box.right");
		assertDoubleEquals(60.0, box.getBottom(), "box.bottom");
		assertDoubleEquals(25.0, box.getCenterX(), "box.centerX");
		assertDoubleEquals(40.0, box.getCenterY(), "box.centerY");
		assertDoubleEquals(1200.0, box.getArea(), "box.area");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testYoloPrediction()
	{
		System.out.println("=== YoloPrediction test ===");

		YoloBoundingBox box = new YoloBoundingBox(10.0, 20.0, 30.0, 40.0);

		YoloPrediction prediction = new YoloPrediction(
				box,
				0.8,
				0.5,
				"person"
		);

		assertDoubleEquals(0.8, prediction.getObjectness(), "prediction.objectness");
		assertDoubleEquals(0.5, prediction.getClassProbability(), "prediction.classProbability");
		assertDoubleEquals(0.4, prediction.getScore(), "prediction.score");

		if(!prediction.isAboveThreshold(0.3))
			throw new IllegalStateException("Prediction should be above threshold 0.3");

		if(prediction.isAboveThreshold(0.5))
			throw new IllegalStateException("Prediction should not be above threshold 0.5");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testYoloGridCell()
	{
		System.out.println("=== YoloGridCell test ===");

		YoloPrediction prediction = new YoloPrediction(
				new YoloBoundingBox(1.0, 2.0, 3.0, 4.0),
				0.9,
				0.8,
				"person"
		);

		YoloGridCell cell = new YoloGridCell(
				1,
				2,
				7,
				0.8,
				List.of(prediction)
		);

		assertIntEquals(1, cell.getRow(), "cell.row");
		assertIntEquals(2, cell.getCol(), "cell.col");
		assertIntEquals(7, cell.getGridSize(), "cell.gridSize");
		assertDoubleEquals(0.8, cell.getPersonProbability(), "cell.personProbability");

		if(!cell.hasPredictions())
			throw new IllegalStateException("Cell should have predictions");

		assertIntEquals(9, cell.getCellIndex(), "cell.index");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testYoloOutput()
	{
		System.out.println("=== YoloOutput test ===");

		int gridSize = 2;
		List<YoloGridCell> cells = new ArrayList<>();

		for(int row = 0; row < gridSize; row++)
		{
			for(int col = 0; col < gridSize; col++)
			{
				List<YoloPrediction> predictions = List.of();

				if(row == 1 && col == 1)
				{
					predictions = List.of(
							new YoloPrediction(
									new YoloBoundingBox(10.0, 10.0, 20.0, 20.0),
									0.9,
									0.9,
									"person"
							)
					);
				}

				cells.add(new YoloGridCell(row, col, gridSize, 0.9, predictions));
			}
		}

		YoloOutput output = new YoloOutput(gridSize, cells);

		assertIntEquals(2, output.getGridSize(), "output.gridSize");
		assertIntEquals(4, output.getCells().size(), "output.cells.size");

		if(!output.hasPredictions())
			throw new IllegalStateException("YoloOutput should have predictions");

		assertIntEquals(1, output.getAllPredictions().size(), "output.allPredictions.size");

		YoloGridCell cell = output.getCell(1, 1);
		if(!cell.hasPredictions())
			throw new IllegalStateException("Cell 1,1 should have predictions");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testYoloRawOutput()
	{
		System.out.println("=== YoloRawOutput test ===");

		int gridSize = 1;
		int boxesPerCell = 1;
		int classCount = 1;

		float[] values = new float[] {
				0.9f,  // class probability
				0.5f,  // x
				0.5f,  // y
				0.25f, // width
				0.25f, // height
				0.8f   // objectness
		};

		YoloRawOutput rawOutput = new YoloRawOutput(
				gridSize,
				boxesPerCell,
				classCount,
				values
		);

		assertIntEquals(6, rawOutput.getExpectedLength(), "raw.expectedLength");
		assertIntEquals(6, rawOutput.getValuesPerCell(), "raw.valuesPerCell");

		assertFloatEquals(0.9f, rawOutput.getValue(0, 0, 0), "raw.classProbability");
		assertFloatEquals(0.5f, rawOutput.getValue(0, 0, 1), "raw.x");
		assertFloatEquals(0.8f, rawOutput.getValue(0, 0, 5), "raw.objectness");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testYoloDecoder()
	{
		System.out.println("=== YoloDecoder test ===");

		float[][][] imageData = new float[2][2][3];

		YoloInput input = new YoloInput(
				imageData,
				640,
				480,
				2,
				2
		);

		YoloRawOutput rawOutput = new YoloRawOutput(
				1,
				1,
				1,
				new float[] {
						0.9f,  // person probability
						0.5f,  // center x inside cell
						0.5f,  // center y inside cell
						0.5f,  // width relative to input
						0.5f,  // height relative to input
						0.8f   // objectness
				}
		);

		YoloOutputDecoder decoder = new YoloOutputDecoder("person");
		YoloOutput output = decoder.decode(rawOutput, input);

		assertIntEquals(1, output.getGridSize(), "decoded.gridSize");
		assertIntEquals(1, output.getCells().size(), "decoded.cells.size");
		assertIntEquals(1, output.getAllPredictions().size(), "decoded.predictions.size");

		YoloPrediction prediction = output.getAllPredictions().get(0);

		assertDoubleEquals(0.72, prediction.getScore(), "decoded.score");

		if(prediction.getWidth() <= 0 || prediction.getHeight() <= 0)
			throw new IllegalStateException("Decoded bounding box must have positive dimensions");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testYoloPostProcessor()
	{
		System.out.println("=== YoloPostProcessor test ===");

		YoloPrediction strongPrediction = new YoloPrediction(
				new YoloBoundingBox(10.0, 10.0, 100.0, 100.0),
				0.9,
				0.9,
				"person"
		);

		YoloPrediction overlappingPrediction = new YoloPrediction(
				new YoloBoundingBox(20.0, 20.0, 100.0, 100.0),
				0.8,
				0.8,
				"person"
		);

		YoloPrediction weakPrediction = new YoloPrediction(
				new YoloBoundingBox(300.0, 300.0, 50.0, 50.0),
				0.1,
				0.1,
				"person"
		);

		List<YoloGridCell> cells = List.of(
				new YoloGridCell(
						0,
						0,
						1,
						0.9,
						List.of(strongPrediction, overlappingPrediction, weakPrediction)
				)
		);

		YoloOutput output = new YoloOutput(1, cells);

		YoloPostProcessor postProcessor = new YoloPostProcessor(
				0.2,
				0.5
		);

		List<YoloPrediction> processed = postProcessor.process(output);

		assertIntEquals(1, processed.size(), "postProcessor.processed.size");
		assertDoubleEquals(strongPrediction.getScore(), processed.get(0).getScore(), "postProcessor.keptScore");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testYoloModel()
	{
		System.out.println("=== YoloModel test ===");

		float[][][] imageData = new float[2][2][3];

		imageData[0][0][0] = 1.0f;
		imageData[0][0][1] = 2.0f;
		imageData[0][0][2] = 3.0f;

		imageData[0][1][0] = 4.0f;
		imageData[0][1][1] = 5.0f;
		imageData[0][1][2] = 6.0f;

		imageData[1][0][0] = 7.0f;
		imageData[1][0][1] = 8.0f;
		imageData[1][0][2] = 9.0f;

		imageData[1][1][0] = 10.0f;
		imageData[1][1][1] = 11.0f;
		imageData[1][1][2] = 12.0f;

		YoloInput input = new YoloInput(
				imageData,
				2,
				2,
				2,
				2
		);

		int flattenedLength = 2 * 2 * 3;
		int hiddenSize = 4;
		int outputSize = 6; // gridSize=1, boxesPerCell=1, classCount=1 => 1*(1+5)

		float[][] hiddenWeights = new float[hiddenSize][flattenedLength];
		float[] hiddenBiases = new float[hiddenSize];

		for(int out = 0; out < hiddenSize; out++)
		{
			for(int in = 0; in < flattenedLength; in++)
			{
				hiddenWeights[out][in] = 0.01f;
			}

			hiddenBiases[out] = 0.0f;
		}

		float[][] outputWeights = new float[outputSize][hiddenSize];
		float[] outputBiases = new float[] {
				0.9f,
				0.5f,
				0.5f,
				0.5f,
				0.5f,
				0.8f
		};

		// Keep weights zero so output equals biases.
		for(int out = 0; out < outputSize; out++)
		{
			for(int in = 0; in < hiddenSize; in++)
			{
				outputWeights[out][in] = 0.0f;
			}
		}

		YoloModel model = new YoloModel(
				1,
				1,
				1,
				List.<Layer<Tensor3D, Tensor3D>>of(),
				new FlattenLayer(),
				new FullyConnectedLayer(hiddenWeights, hiddenBiases),
				new Activation1DLayer(),
				new FullyConnectedLayer(outputWeights, outputBiases)
		);

		YoloRawOutput rawOutput = model.forward(input);

		assertIntEquals(6, rawOutput.getExpectedLength(), "model.rawOutput.expectedLength");
		assertFloatEquals(0.9f, rawOutput.getValue(0, 0, 0), "model.rawOutput.classProbability");
		assertFloatEquals(0.5f, rawOutput.getValue(0, 0, 1), "model.rawOutput.x");
		assertFloatEquals(0.8f, rawOutput.getValue(0, 0, 5), "model.rawOutput.objectness");

		System.out.println("PASS");
		System.out.println();
	}

	private static void assertIntEquals(int expected, int actual, String label)
	{
		if(expected != actual)
			throw new IllegalStateException(label + " expected " + expected + " but got " + actual);
	}

	private static void assertFloatEquals(float expected, float actual, String label)
	{
		float epsilon = 0.0001f;

		if(Math.abs(expected - actual) > epsilon)
			throw new IllegalStateException(label + " expected " + expected + " but got " + actual);
	}

	private static void assertDoubleEquals(double expected, double actual, String label)
	{
		double epsilon = 0.0001;

		if(Math.abs(expected - actual) > epsilon)
			throw new IllegalStateException(label + " expected " + expected + " but got " + actual);
	}
}