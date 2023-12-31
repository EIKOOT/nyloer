package com.nyloer.stats;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public
class Stall
{
	private int wave;
	private int aliveCount;
	private int capSize;
	private int totalStalls;

	public Stall(int wave, int aliveCount, int capSize, int totalStalls)
	{
		this.wave = wave;
		this.aliveCount = aliveCount;
		this.capSize = capSize;
		this.totalStalls = totalStalls;
	}
}