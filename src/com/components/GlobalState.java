package com.components;

import java.util.ArrayList;

import android.location.Location;

public class GlobalState {

	public static  ArrayList<GPSData> hills;
	public static GPSData currentHill;
	public static int level;
	public static int numPlayers;
	public static String overrideCode;
	public static ArrayList<Integer[]> hillOrders;
}
