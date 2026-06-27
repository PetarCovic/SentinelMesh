package com.sentinelmesh.edge.yolo.v1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.sentinelmesh.edge.cnn.Activation1DLayer;
import com.sentinelmesh.edge.cnn.FlattenLayer;
import com.sentinelmesh.edge.cnn.FullyConnectedLayer;
import com.sentinelmesh.edge.cnn.Layer;
import com.sentinelmesh.edge.cnn.Padding;
import com.sentinelmesh.edge.cnn.Tensor3D;
import com.sentinelmesh.edge.cnn.WeightInitializer;
import com.sentinelmesh.edge.yolo.YoloBoundingBox;
import com.sentinelmesh.edge.yolo.YoloInput;
import com.sentinelmesh.edge.yolo.YoloPostProcessor;
import com.sentinelmesh.edge.yolo.YoloPrediction;

public class YoloV1SanityTest 
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
		testYoloV1RandomPersonModel();
		testYoloV1WeightsWriteLoad();
		testMultiConvWeightsWriteLoadAndModelForward();
		testYoloV1ModelFactoryCreateFromWeightsPath();

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

		YoloV1GridCell cell = new YoloV1GridCell(
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
		List<YoloV1GridCell> cells = new ArrayList<>();

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

				cells.add(new YoloV1GridCell(row, col, gridSize, 0.9, predictions));
			}
		}

		YoloV1Output output = new YoloV1Output(gridSize, cells);

		assertIntEquals(2, output.getGridSize(), "output.gridSize");
		assertIntEquals(4, output.getCells().size(), "output.cells.size");

		if(!output.hasPredictions())
			throw new IllegalStateException("YoloOutput should have predictions");

		assertIntEquals(1, output.getAllPredictions().size(), "output.allPredictions.size");

		YoloV1GridCell cell = output.getCell(1, 1);
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

		YoloV1RawOutput rawOutput = new YoloV1RawOutput(
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

		YoloV1RawOutput rawOutput = new YoloV1RawOutput(
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

		YoloV1OutputDecoder decoder = new YoloV1OutputDecoder("person");
		YoloV1Output output = decoder.decode(rawOutput, input);

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

		List<YoloV1GridCell> cells = List.of(
				new YoloV1GridCell(
						0,
						0,
						1,
						0.9,
						List.of(strongPrediction, overlappingPrediction, weakPrediction)
				)
		);

		YoloV1Output output = new YoloV1Output(1, cells);

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

		YoloV1Model model = new YoloV1Model(
				1,
				1,
				1,
				List.<Layer<Tensor3D, Tensor3D>>of(),
				new FlattenLayer(),
				new FullyConnectedLayer(hiddenWeights, hiddenBiases),
				new Activation1DLayer(),
				new FullyConnectedLayer(outputWeights, outputBiases)
		);

		YoloV1RawOutput rawOutput = model.forward(input);

		assertIntEquals(6, rawOutput.getExpectedLength(), "model.rawOutput.expectedLength");
		assertFloatEquals(0.9f, rawOutput.getValue(0, 0, 0), "model.rawOutput.classProbability");
		assertFloatEquals(0.5f, rawOutput.getValue(0, 0, 1), "model.rawOutput.x");
		assertFloatEquals(0.8f, rawOutput.getValue(0, 0, 5), "model.rawOutput.objectness");

		System.out.println("PASS");
		System.out.println();
	}
	
	private static void testYoloV1RandomPersonModel()
	{
		System.out.println("=== YoloV1 RandomPersonModel test ===");

		int inputHeight = 448;
		int inputWidth = 448;
		int inputChannels = 3;

		float[][][] imageData = new float[inputHeight][inputWidth][inputChannels];

		for(int y = 0; y < inputHeight; y++)
		{
			for(int x = 0; x < inputWidth; x++)
			{
				for(int c = 0; c < inputChannels; c++)
				{
					imageData[y][x][c] = 0.5f;
				}
			}
		}

		YoloInput input = new YoloInput(
				imageData,
				inputWidth,
				inputHeight,
				inputWidth,
				inputHeight
		);

		YoloV1Model model = YoloV1ModelFactory.createRandomPersonModel();

		YoloV1RawOutput rawOutput = model.forward(input);

		int expectedLength = 7 * 7 * (1 + 2 * 5);

		assertIntEquals(expectedLength, rawOutput.getExpectedLength(), "randomModel.rawOutput.expectedLength");
		assertIntEquals(expectedLength, rawOutput.getValues().length, "randomModel.rawOutput.values.length");

		YoloV1OutputDecoder decoder = new YoloV1OutputDecoder("person");
		YoloV1Output decodedOutput = decoder.decode(rawOutput, input);

		assertIntEquals(7, decodedOutput.getGridSize(), "randomModel.decodedOutput.gridSize");
		assertIntEquals(49, decodedOutput.getCells().size(), "randomModel.decodedOutput.cells.size");

		YoloPostProcessor postProcessor = new YoloPostProcessor(0.0, 0.45);
		List<YoloPrediction> predictions = postProcessor.process(decodedOutput);

		if(predictions == null)
			throw new IllegalStateException("Random model predictions should not be null");

		System.out.println("Raw output length: " + rawOutput.getValues().length);
		System.out.println("Decoded cells: " + decodedOutput.getCells().size());
		System.out.println("Post-processed predictions: " + predictions.size());

		System.out.println("PASS");
		System.out.println();
	}
	
	private static void testYoloV1WeightsWriteLoad()
	{
		System.out.println("=== YoloV1 Weights write/load test ===");

		YoloV1ConvLayerWeights conv1 = new YoloV1ConvLayerWeights(
				WeightInitializer.createConvFilters(
						16,
						7,
						7,
						3,
						-0.01f,
						0.01f
				),
				WeightInitializer.createBiases(16, 0.0f),
				2,
				Padding.SAME,
				true,
				true,
				2,
				2
		);

		YoloV1ConvLayerWeights conv2 = new YoloV1ConvLayerWeights(
				WeightInitializer.createConvFilters(
						32,
						3,
						3,
						16,
						-0.01f,
						0.01f
				),
				WeightInitializer.createBiases(32, 0.0f),
				1,
				Padding.SAME,
				true,
				true,
				2,
				2
		);

		int flattenedLength = 56 * 56 * 32;
		int hiddenSize = 128;
		int outputSize = 7 * 7 * (1 + 2 * 5);

		float[][] hiddenWeights = WeightInitializer.createFullyConnectedWeights(
				hiddenSize,
				flattenedLength,
				-0.01f,
				0.01f
		);

		float[] hiddenBiases = WeightInitializer.createBiases(hiddenSize, 0.0f);

		float[][] outputWeights = WeightInitializer.createFullyConnectedWeights(
				outputSize,
				hiddenSize,
				-0.01f,
				0.01f
		);

		float[] outputBiases = WeightInitializer.createBiases(outputSize, 0.0f);

		YoloV1Weights originalWeights = new YoloV1Weights(
				List.of(conv1, conv2),
				hiddenWeights,
				hiddenBiases,
				outputWeights,
				outputBiases
		);

		try
		{
			Path tempFile = Files.createTempFile("yolo-v1-test-", ".weights");

			YoloV1WeightsWriter writer = new YoloV1WeightsWriter();
			writer.write(originalWeights, tempFile);

			YoloV1WeightsLoader loader = new YoloV1WeightsLoader();
			YoloV1Weights loadedWeights = loader.load(tempFile);

			assertIntEquals(2, loadedWeights.getConvLayers().size(), "loaded.convLayers.size");

			YoloV1ConvLayerWeights loadedConv1 = loadedWeights.getConvLayers().get(0);
			YoloV1ConvLayerWeights loadedConv2 = loadedWeights.getConvLayers().get(1);

			assert4DShape(loadedConv1.getFilters(), 16, 7, 7, 3, "loaded.conv1.filters");
			assert1DLength(loadedConv1.getBiases(), 16, "loaded.conv1.biases");
			assertIntEquals(2, loadedConv1.getStride(), "loaded.conv1.stride");

			if(loadedConv1.getPadding() != Padding.SAME)
				throw new IllegalStateException("loaded.conv1.padding expected SAME but got " + loadedConv1.getPadding());

			if(!loadedConv1.isApplyLeakyReluAfter())
				throw new IllegalStateException("loaded.conv1 should apply LeakyReLU");

			if(!loadedConv1.isApplyMaxPoolAfter())
				throw new IllegalStateException("loaded.conv1 should apply MaxPool");

			assertIntEquals(2, loadedConv1.getMaxPoolSize(), "loaded.conv1.maxPoolSize");
			assertIntEquals(2, loadedConv1.getMaxPoolStride(), "loaded.conv1.maxPoolStride");

			assert4DShape(loadedConv2.getFilters(), 32, 3, 3, 16, "loaded.conv2.filters");
			assert1DLength(loadedConv2.getBiases(), 32, "loaded.conv2.biases");
			assertIntEquals(1, loadedConv2.getStride(), "loaded.conv2.stride");

			if(loadedConv2.getPadding() != Padding.SAME)
				throw new IllegalStateException("loaded.conv2.padding expected SAME but got " + loadedConv2.getPadding());

			if(!loadedConv2.isApplyLeakyReluAfter())
				throw new IllegalStateException("loaded.conv2 should apply LeakyReLU");

			if(!loadedConv2.isApplyMaxPoolAfter())
				throw new IllegalStateException("loaded.conv2 should apply MaxPool");

			assertIntEquals(2, loadedConv2.getMaxPoolSize(), "loaded.conv2.maxPoolSize");
			assertIntEquals(2, loadedConv2.getMaxPoolStride(), "loaded.conv2.maxPoolStride");

			assert2DShape(loadedWeights.getHiddenWeights(), hiddenSize, flattenedLength, "loaded.hiddenWeights");
			assert1DLength(loadedWeights.getHiddenBiases(), hiddenSize, "loaded.hiddenBiases");

			assert2DShape(loadedWeights.getOutputWeights(), outputSize, hiddenSize, "loaded.outputWeights");
			assert1DLength(loadedWeights.getOutputBiases(), outputSize, "loaded.outputBiases");

			assertFloatEquals(
					originalWeights.getConvLayers().get(0).getFilters()[0][0][0][0],
					loadedWeights.getConvLayers().get(0).getFilters()[0][0][0][0],
					"conv1.filters[0][0][0][0]"
			);

			assertFloatEquals(
					originalWeights.getConvLayers().get(1).getFilters()[0][0][0][0],
					loadedWeights.getConvLayers().get(1).getFilters()[0][0][0][0],
					"conv2.filters[0][0][0][0]"
			);

			assertFloatEquals(
					originalWeights.getHiddenWeights()[0][0],
					loadedWeights.getHiddenWeights()[0][0],
					"hiddenWeights[0][0]"
			);

			assertFloatEquals(
					originalWeights.getOutputWeights()[0][0],
					loadedWeights.getOutputWeights()[0][0],
					"outputWeights[0][0]"
			);

			Files.deleteIfExists(tempFile);

			System.out.println("PASS");
			System.out.println();
		}
		catch(IOException ex)
		{
			throw new IllegalStateException("Weights write/load sanity test failed", ex);
		}
	}
	
	private static void testMultiConvWeightsWriteLoadAndModelForward()
	{
		try
		{
			Path tempFile = Files.createTempFile("sentinelmesh-yolov1-multiconv-", ".weights");
			
			int gridSize = 7;
			int boxesPerCell = 2;
			int classCount = 1;
			
			int flattenedLength = 56 * 56 * 32;
			int hiddenSize = 128;
			int outputSize = gridSize * gridSize * (classCount + boxesPerCell * 5);
			
			YoloV1ConvLayerWeights conv1 = new YoloV1ConvLayerWeights(
					WeightInitializer.createConvFilters(16, 7, 7, 3, -0.01f, 0.01f),
					WeightInitializer.createBiases(16, 0.0f),
					2,
					Padding.SAME,
					true,
					true,
					2,
					2
			);
			
			YoloV1ConvLayerWeights conv2 = new YoloV1ConvLayerWeights(
					WeightInitializer.createConvFilters(32, 3, 3, 16, -0.01f, 0.01f),
					WeightInitializer.createBiases(32, 0.0f),
					1,
					Padding.SAME,
					true,
					true,
					2,
					2
			);
			
			float[][] hiddenWeights = WeightInitializer.createFullyConnectedWeights(
					hiddenSize,
					flattenedLength,
					-0.01f,
					0.01f
			);
			
			float[] hiddenBiases = WeightInitializer.createBiases(hiddenSize, 0.0f);
			
			float[][] outputWeights = createZeroWeights(outputSize, hiddenSize);
			float[] outputBiases = createTestOutputBiases(gridSize, boxesPerCell, classCount);
			
			YoloV1Weights weights = new YoloV1Weights(
					List.of(conv1, conv2),
					hiddenWeights,
					hiddenBiases,
					outputWeights,
					outputBiases
			);
			
			new YoloV1WeightsWriter().write(weights, tempFile);
			
			YoloV1Weights loadedWeights = new YoloV1WeightsLoader().load(tempFile);
			
			assertTrue(loadedWeights.getConvLayers().size() == 2, "Loaded weights should contain 2 conv layers");
			
			assertTrue(loadedWeights.getConvLayers().get(0).getFilters().length == 16, "Conv1 should have 16 filters");
			assertTrue(loadedWeights.getConvLayers().get(1).getFilters().length == 32, "Conv2 should have 32 filters");
			
			assertTrue(loadedWeights.getHiddenWeights().length == hiddenSize, "Hidden output size should match");
			assertTrue(loadedWeights.getHiddenWeights()[0].length == flattenedLength, "Hidden input size should match");
			
			assertTrue(loadedWeights.getOutputWeights().length == outputSize, "Output size should match");
			assertTrue(loadedWeights.getOutputWeights()[0].length == hiddenSize, "Output input size should match");
			
			YoloV1Model model = YoloV1ModelFactory.createFromWeights(loadedWeights);
			
			YoloInput input = createFakeInput(448, 448);
			YoloV1RawOutput rawOutput = model.forward(input);
			
			assertTrue(rawOutput.getValues().length == outputSize, "Raw output should have 539 values");
			
			YoloV1Output decodedOutput = new YoloV1OutputDecoder("person").decode(rawOutput, input);
			
			assertTrue(decodedOutput.getGridSize() == 7, "Decoded output grid size should be 7");
			assertTrue(decodedOutput.getCells().size() == 49, "Decoded output should contain 49 cells");
			
			Files.deleteIfExists(tempFile);
			
			System.out.println("testMultiConvWeightsWriteLoadAndModelForward passed");
		}
		catch(IOException ex)
		{
			throw new RuntimeException("Failed multi-conv weight test", ex);
		}
	}
	
	private static void testYoloV1ModelFactoryCreateFromWeightsPath()
	{
		System.out.println("=== YoloV1ModelFactory createFromWeights(Path) test ===");

		YoloV1ConvLayerWeights conv1 = new YoloV1ConvLayerWeights(
				WeightInitializer.createConvFilters(
						16,
						7,
						7,
						3,
						-0.01f,
						0.01f
				),
				WeightInitializer.createBiases(16, 0.0f),
				2,
				Padding.SAME,
				true,
				true,
				2,
				2
		);

		YoloV1ConvLayerWeights conv2 = new YoloV1ConvLayerWeights(
				WeightInitializer.createConvFilters(
						32,
						3,
						3,
						16,
						-0.01f,
						0.01f
				),
				WeightInitializer.createBiases(32, 0.0f),
				1,
				Padding.SAME,
				true,
				true,
				2,
				2
		);

		int flattenedLength = 56 * 56 * 32;
		int hiddenSize = 128;
		int outputSize = 7 * 7 * (1 + 2 * 5);

		float[][] hiddenWeights = WeightInitializer.createFullyConnectedWeights(
				hiddenSize,
				flattenedLength,
				-0.01f,
				0.01f
		);

		float[] hiddenBiases = WeightInitializer.createBiases(hiddenSize, 0.0f);

		float[][] outputWeights = WeightInitializer.createFullyConnectedWeights(
				outputSize,
				hiddenSize,
				-0.01f,
				0.01f
		);

		float[] outputBiases = createTestOutputBiases(7, 2, 1);

		YoloV1Weights weights = new YoloV1Weights(
				List.of(conv1, conv2),
				hiddenWeights,
				hiddenBiases,
				outputWeights,
				outputBiases
		);

		try
		{
			Path tempFile = Files.createTempFile("yolo-v1-model-factory-test-", ".weights");

			YoloV1WeightsWriter writer = new YoloV1WeightsWriter();
			writer.write(weights, tempFile);

			YoloV1Model model = YoloV1ModelFactory.createFromWeights(tempFile);

			int inputHeight = 448;
			int inputWidth = 448;
			int inputChannels = 3;

			float[][][] imageData = new float[inputHeight][inputWidth][inputChannels];

			for(int y = 0; y < inputHeight; y++)
			{
				for(int x = 0; x < inputWidth; x++)
				{
					for(int c = 0; c < inputChannels; c++)
					{
						imageData[y][x][c] = 0.5f;
					}
				}
			}

			YoloInput input = new YoloInput(
					imageData,
					inputWidth,
					inputHeight,
					inputWidth,
					inputHeight
			);

			YoloV1RawOutput rawOutput = model.forward(input);

			assertIntEquals(outputSize, rawOutput.getExpectedLength(), "factory.rawOutput.expectedLength");
			assertIntEquals(outputSize, rawOutput.getValues().length, "factory.rawOutput.values.length");

			YoloV1OutputDecoder decoder = new YoloV1OutputDecoder("person");
			YoloV1Output decodedOutput = decoder.decode(rawOutput, input);

			assertIntEquals(7, decodedOutput.getGridSize(), "factory.decodedOutput.gridSize");
			assertIntEquals(49, decodedOutput.getCells().size(), "factory.decodedOutput.cells.size");

			YoloPostProcessor postProcessor = new YoloPostProcessor(0.0, 0.45);
			List<YoloPrediction> predictions = postProcessor.process(decodedOutput);

			if(predictions == null)
				throw new IllegalStateException("Predictions cannot be null");

			Files.deleteIfExists(tempFile);

			System.out.println("Raw output length: " + rawOutput.getValues().length);
			System.out.println("Decoded cells: " + decodedOutput.getCells().size());
			System.out.println("Post-processed predictions: " + predictions.size());

			System.out.println("PASS");
			System.out.println();
		}
		catch(IOException ex)
		{
			throw new IllegalStateException("YoloV1ModelFactory createFromWeights(Path) sanity test failed", ex);
		}
	}
	
	private static float[][] createZeroWeights(int outputSize, int inputSize)
	{
		float[][] weights = new float[outputSize][inputSize];
		return weights;
	}
	
	private static float[] createTestOutputBiases(int gridSize, int boxesPerCell, int classCount)
	{
		int valuesPerCell = classCount + boxesPerCell * 5;
		int outputSize = gridSize * gridSize * valuesPerCell;
		
		float[] biases = new float[outputSize];
		
		for(int cellIndex = 0; cellIndex < gridSize * gridSize; cellIndex++)
		{
			int cellStart = cellIndex * valuesPerCell;
			
			biases[cellStart] = 0.5f;
			
			for(int boxIndex = 0; boxIndex < boxesPerCell; boxIndex++)
			{
				int boxStart = cellStart + classCount + boxIndex * 5;
				
				biases[boxStart] = 0.5f;
				biases[boxStart + 1] = 0.5f;
				biases[boxStart + 2] = 0.2f;
				biases[boxStart + 3] = 0.2f;
				biases[boxStart + 4] = 0.5f;
			}
		}
		
		return biases;
	}
	
	private static YoloInput createFakeInput(int width, int height)
	{
		float[][][] imageData = new float[height][width][3];
		
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				imageData[y][x][0] = 0.5f;
				imageData[y][x][1] = 0.5f;
				imageData[y][x][2] = 0.5f;
			}
		}
		
		return new YoloInput(imageData, width, height, width, height);
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
	
	private static void assert4DShape(
			float[][][][] values,
			int expectedDim0,
			int expectedDim1,
			int expectedDim2,
			int expectedDim3,
			String label)
	{
		if(values.length != expectedDim0 ||
				values[0].length != expectedDim1 ||
				values[0][0].length != expectedDim2 ||
				values[0][0][0].length != expectedDim3)
		{
			throw new IllegalStateException(
					label + " expected shape " +
					expectedDim0 + "x" + expectedDim1 + "x" + expectedDim2 + "x" + expectedDim3 +
					" but got " +
					values.length + "x" + values[0].length + "x" + values[0][0].length + "x" + values[0][0][0].length
			);
		}
	}

	private static void assert2DShape(
			float[][] values,
			int expectedRows,
			int expectedCols,
			String label)
	{
		if(values.length != expectedRows || values[0].length != expectedCols)
		{
			throw new IllegalStateException(
					label + " expected shape " +
					expectedRows + "x" + expectedCols +
					" but got " +
					values.length + "x" + values[0].length
			);
		}
	}

	private static void assert1DLength(float[] values, int expectedLength, String label)
	{
		if(values.length != expectedLength)
		{
			throw new IllegalStateException(
					label + " expected length " + expectedLength +
					" but got " + values.length
			);
		}
	}
	
	private static void assertTrue(boolean condition, String message)
	{
		if(!condition)
			throw new AssertionError(message);
	}
}