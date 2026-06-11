package com.sentinelmesh.edge.cnn;

public class CNNSanityTest 
{
	public static void main(String[] args) 
	{
		testConvolutionLayerValidPadding();
		testMaxPoolLayer();
		testFlattenLayer();
		testFullyConnectedLayer();
		testLeakyReluLayer();

		System.out.println();
		System.out.println("All CNN sanity tests completed.");
	}

	private static void testConvolutionLayerValidPadding()
	{
		System.out.println("=== ConvolutionLayer VALID padding test ===");

		Tensor3D input = new Tensor3D(3, 3, 1);

		input.set(0, 0, 0, 1.0f);
		input.set(0, 1, 0, 2.0f);
		input.set(0, 2, 0, 3.0f);

		input.set(1, 0, 0, 4.0f);
		input.set(1, 1, 0, 5.0f);
		input.set(1, 2, 0, 6.0f);

		input.set(2, 0, 0, 7.0f);
		input.set(2, 1, 0, 8.0f);
		input.set(2, 2, 0, 9.0f);

		float[][][][] filters = new float[1][2][2][1];

		filters[0][0][0][0] = 1.0f;
		filters[0][0][1][0] = 1.0f;
		filters[0][1][0][0] = 1.0f;
		filters[0][1][1][0] = 1.0f;

		float[] biases = new float[] { 0.0f };

		ConvolutionLayer layer = new ConvolutionLayer(
				filters,
				biases,
				1,
				Padding.VALID
		);

		Tensor3D output = layer.forward(input);

		assertShape(output, 2, 2, 1, "Convolution output shape");

		assertFloatEquals(12.0f, output.get(0, 0, 0), "conv[0][0][0]");
		assertFloatEquals(16.0f, output.get(0, 1, 0), "conv[0][1][0]");
		assertFloatEquals(24.0f, output.get(1, 0, 0), "conv[1][0][0]");
		assertFloatEquals(28.0f, output.get(1, 1, 0), "conv[1][1][0]");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testMaxPoolLayer()
	{
		System.out.println("=== MaxPoolLayer test ===");

		Tensor3D input = new Tensor3D(4, 4, 1);

		float value = 1.0f;

		for(int y = 0; y < 4; y++)
		{
			for(int x = 0; x < 4; x++)
			{
				input.set(y, x, 0, value);
				value++;
			}
		}

		MaxPoolLayer layer = new MaxPoolLayer(2, 2);

		Tensor3D output = layer.forward(input);

		assertShape(output, 2, 2, 1, "MaxPool output shape");

		assertFloatEquals(6.0f, output.get(0, 0, 0), "pool[0][0][0]");
		assertFloatEquals(8.0f, output.get(0, 1, 0), "pool[0][1][0]");
		assertFloatEquals(14.0f, output.get(1, 0, 0), "pool[1][0][0]");
		assertFloatEquals(16.0f, output.get(1, 1, 0), "pool[1][1][0]");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testFlattenLayer()
	{
		System.out.println("=== FlattenLayer test ===");

		Tensor3D input = new Tensor3D(2, 2, 2);

		input.set(0, 0, 0, 1.0f);
		input.set(0, 0, 1, 2.0f);

		input.set(0, 1, 0, 3.0f);
		input.set(0, 1, 1, 4.0f);

		input.set(1, 0, 0, 5.0f);
		input.set(1, 0, 1, 6.0f);

		input.set(1, 1, 0, 7.0f);
		input.set(1, 1, 1, 8.0f);

		FlattenLayer layer = new FlattenLayer();

		Tensor1D output = layer.forward(input);

		if(output.getLength() != 8)
			throw new IllegalStateException("Flatten output length expected 8 but got " + output.getLength());

		assertFloatEquals(1.0f, output.get(0), "flatten[0]");
		assertFloatEquals(2.0f, output.get(1), "flatten[1]");
		assertFloatEquals(3.0f, output.get(2), "flatten[2]");
		assertFloatEquals(4.0f, output.get(3), "flatten[3]");
		assertFloatEquals(5.0f, output.get(4), "flatten[4]");
		assertFloatEquals(6.0f, output.get(5), "flatten[5]");
		assertFloatEquals(7.0f, output.get(6), "flatten[6]");
		assertFloatEquals(8.0f, output.get(7), "flatten[7]");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testFullyConnectedLayer()
	{
		System.out.println("=== FullyConnectedLayer test ===");

		Tensor1D input = new Tensor1D(3);

		input.set(0, 1.0f);
		input.set(1, 2.0f);
		input.set(2, 3.0f);

		/*
		 * weights[outputIndex][inputIndex]
		 *
		 * output0 = 0.5 + 1*1 + 2*1 + 3*1 = 6.5
		 * output1 = 1.0 + 1*2 + 2*0 + 3*(-1) = 0.0
		 */
		float[][] weights = new float[][] {
				{ 1.0f, 1.0f, 1.0f },
				{ 2.0f, 0.0f, -1.0f }
		};

		float[] biases = new float[] {
				0.5f,
				1.0f
		};

		FullyConnectedLayer layer = new FullyConnectedLayer(weights, biases);

		Tensor1D output = layer.forward(input);

		if(output.getLength() != 2)
			throw new IllegalStateException("FullyConnected output length expected 2 but got " + output.getLength());

		assertFloatEquals(6.5f, output.get(0), "fc[0]");
		assertFloatEquals(0.0f, output.get(1), "fc[1]");

		System.out.println("PASS");
		System.out.println();
	}

	private static void testLeakyReluLayer()
	{
		System.out.println("=== LeakyReluLayer test ===");

		Tensor3D input = new Tensor3D(1, 4, 1);

		input.set(0, 0, 0, -10.0f);
		input.set(0, 1, 0, -1.0f);
		input.set(0, 2, 0, 0.0f);
		input.set(0, 3, 0, 5.0f);

		LeakyReluLayer layer = new LeakyReluLayer(new LeakyReluActivation(0.1f));

		Tensor3D output = layer.forward(input);

		assertShape(output, 1, 4, 1, "LeakyReLU output shape");

		assertFloatEquals(-1.0f, output.get(0, 0, 0), "relu[0][0][0]");
		assertFloatEquals(-0.1f, output.get(0, 1, 0), "relu[0][1][0]");
		assertFloatEquals(0.0f, output.get(0, 2, 0), "relu[0][2][0]");
		assertFloatEquals(5.0f, output.get(0, 3, 0), "relu[0][3][0]");

		System.out.println("PASS");
		System.out.println();
	}

	private static void assertShape(
			Tensor3D tensor,
			int expectedHeight,
			int expectedWidth,
			int expectedChannels,
			String label)
	{
		if(tensor.getHeight() != expectedHeight ||
				tensor.getWidth() != expectedWidth ||
				tensor.getChannels() != expectedChannels)
		{
			throw new IllegalStateException(
					label + " expected " +
							expectedHeight + "x" + expectedWidth + "x" + expectedChannels +
							" but got " +
							tensor.getHeight() + "x" + tensor.getWidth() + "x" + tensor.getChannels()
			);
		}
	}

	private static void assertFloatEquals(float expected, float actual, String label)
	{
		float epsilon = 0.0001f;

		if(Math.abs(expected - actual) > epsilon)
		{
			throw new IllegalStateException(
					label + " expected " + expected + " but got " + actual
			);
		}
	}
}