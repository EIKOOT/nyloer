package com.nyloer.stats;


public class Stats
{
	public String totalTime;
	public String bossTime;
	public String wavesTime;

	public int stallCountPre;
	public int stallCount1to12;
	public int stallCount13to19;
	public int stallCount21;
	public int stallCount22to27;
	public int stallCount28;
	public int stallCount29;
	public int stallCount30;

	public int bigsAlive22;
	public int bigsAlive29;
	public int bigsAlive30;
	public int bigsAlive31;

	public int[] bossRotation;

	Stats()
	{
		bossRotation = new int[3];
		reset();
	}

	public void reset()
	{
		totalTime = "";
		bossTime = "";
		wavesTime = "";

		bossRotation[0] = 1;
		bossRotation[1] = 0;
		bossRotation[2] = 0;

		stallCountPre = 0;
		stallCount1to12 = 0;
		stallCount13to19 = 0;
		stallCount21 = 0;
		stallCount22to27 = 0;
		stallCount28 = 0;
		stallCount29 = 0;
		stallCount30 = 0;

		bigsAlive22 = 0;
		bigsAlive29 = 0;
		bigsAlive30 = 0;
		bigsAlive31 = 0;
	}
}