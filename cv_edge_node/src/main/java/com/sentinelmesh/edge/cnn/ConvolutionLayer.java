package com.sentinelmesh.edge.cnn;

public class ConvolutionLayer implements Layer<Tensor3D, Tensor3D>
{
	private final float[][][][] filters;
	private final float[] biases;
	private final int stride;
	private final Padding padding;
	private final int filterCount;
	private final int kernelHeight;
	private final int kernelWidth;
	private final int inputChannels;

	public ConvolutionLayer(float[][][][] filters, float[] biases, int stride, Padding padding)
	{
		if(filters == null || filters.length == 0)
			throw new IllegalArgumentException("Filters cannot be null or empty");

		if(biases == null || biases.length == 0)
			throw new IllegalArgumentException("Biases cannot be null or empty");

		if(filters.length != biases.length)
			throw new IllegalArgumentException("Filters length must equal biases length");

		if(stride <= 0)
			throw new IllegalArgumentException("Stride must be greater than 0");

		if(padding == null)
			throw new IllegalArgumentException("Padding cannot be null");

		if(filters[0] == null || filters[0].length == 0)
			throw new IllegalArgumentException("Filters[0] cannot be null or empty");

		if(filters[0][0] == null || filters[0][0].length == 0)
			throw new IllegalArgumentException("Filters[0][0] cannot be null or empty");

		if(filters[0][0][0] == null || filters[0][0][0].length == 0)
			throw new IllegalArgumentException("Filters[0][0][0] cannot be null or empty");
		
		this.filterCount=filters.length;
		this.kernelHeight=filters[0].length;
		this.kernelWidth=filters[0][0].length;
		this.inputChannels=filters[0][0][0].length;
		
		for(int f = 0; f < filters.length; f++)
		{
			if(filters[f] == null || filters[f].length != kernelHeight)
				throw new IllegalArgumentException("Filter " + f + " must have kernelHeight " + kernelHeight);
			
			for(int ky = 0; ky < kernelHeight; ky++)
			{
				if(filters[f][ky] == null || filters[f][ky].length != kernelWidth)
					throw new IllegalArgumentException("Filter " + f + ", kernel row " + ky + " must have kernelWidth " + kernelWidth);
				
				for(int kx = 0; kx < kernelWidth; kx++)
				{
					if(filters[f][ky][kx] == null || filters[f][ky][kx].length != inputChannels)
						throw new IllegalArgumentException(
								"Filter " + f + ", kernel position (" + ky + ", " + kx + ") must have inputChannels " + inputChannels
						);
				}
			}
		}
		
		this.filters=filters;
		this.biases=biases;
		this.stride=stride;
		this.padding=padding;
	}
	
	private int calculateOutputHeight(int inputHeight)
	{
		if(padding==Padding.VALID)
			return ((inputHeight-kernelHeight)/stride)+1;
		
		return (inputHeight+stride-1)/stride;
	}
	
	private int calculateOutputWidth(int inputWidth)
	{
		if(padding==Padding.VALID)
			return ((inputWidth-kernelWidth)/stride)+1;
		
		return (inputWidth+stride-1)/stride;
	}
	
	private int calculatePadTop(int inputHeight)
	{
		if(padding==Padding.VALID)
			return 0;
		
		int neededInputHeight=(calculateOutputHeight(inputHeight)-1)*stride+kernelHeight;
		int totalPadHeight=Math.max(0, neededInputHeight-inputHeight);
		
		return totalPadHeight/2;
	}
	
	private int calculatePadLeft(int inputWidth)
	{
		if(padding==Padding.VALID)
			return 0;
		
		int neededInputWidth=(calculateOutputWidth(inputWidth)-1)*stride+kernelWidth;
		int totalPadWidth=Math.max(0, neededInputWidth-inputWidth);
		
		return totalPadWidth/2;
	}
	
	private float getInputOrZero(Tensor3D input, int y, int x, int channel)
	{
		if(y < 0 || y >= input.getHeight())
	        return 0.0f;

	    if(x < 0 || x >= input.getWidth())
	        return 0.0f;

	    return input.get(y, x, channel);
	}

	@Override
	public Tensor3D forward(Tensor3D input) 
	{
		if(input==null)
			throw new IllegalArgumentException("Input cannot be null");
		
		if(input.getChannels()!=inputChannels)
			throw new IllegalArgumentException("Input channels must match layer input channels");
		
		if(padding == Padding.VALID && input.getHeight() < kernelHeight)
		    throw new IllegalArgumentException("Input height must be at least kernelHeight for VALID padding");

		if(padding == Padding.VALID && input.getWidth() < kernelWidth)
		    throw new IllegalArgumentException("Input width must be at least kernelWidth for VALID padding");
		
		int outputHeight=calculateOutputHeight(input.getHeight());
		int outputWidth=calculateOutputWidth(input.getWidth());
		
		int padTop=calculatePadTop(input.getHeight());
		int padLeft=calculatePadLeft(input.getWidth());
		
		Tensor3D output=new Tensor3D(outputHeight, outputWidth, filterCount);
		
		for(int outputY=0; outputY<outputHeight; outputY++)
		{
			for(int outputX=0; outputX<outputWidth; outputX++)
			{
				for(int filterIndex=0; filterIndex<filterCount; filterIndex++)
				{
					float sum=biases[filterIndex];
					
					for(int kernelY=0; kernelY<kernelHeight; kernelY++)
					{
						for(int kernelX=0; kernelX<kernelWidth; kernelX++)
						{
							for(int inputChannel=0; inputChannel<inputChannels; inputChannel++)
							{
								int inputY=outputY*stride+kernelY-padTop;
								int inputX=outputX*stride+kernelX-padLeft;
								
								float inputValue=getInputOrZero(input, inputY, inputX, inputChannel);
								float weight=filters[filterIndex][kernelY][kernelX][inputChannel];
								
								sum+=inputValue*weight;
							}
						}
					}
					
					output.set(outputY, outputX, filterIndex, sum);
				}
			}
		}
		
		return output;
	}
	
	public int getFilterCount()
	{
		return filterCount;
	}
	
	public int getKernelHeight()
	{
		return kernelHeight;
	}
	
	public int getKernelWidth()
	{
		return kernelWidth;
	}
	
	public int getInputChannels()
	{
		return inputChannels;
	}
	
	public int getStride()
	{
		return stride;
	}
	
	public Padding getPadding()
	{
		return padding;
	}
}