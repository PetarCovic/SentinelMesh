package com.sentinelmesh.edge.cnn;

public interface Layer<I, O>
{
	O forward(I input);
}
