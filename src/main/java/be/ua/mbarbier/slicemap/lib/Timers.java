/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ua.mbarbier.slicemap.lib;

import ij.IJ;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 *
 * @author mbarbier
 */
public class Timers {

	public class Timer {
		
		String name = "";
		long millisStart;
		long millisStop;
		double elapsedTime;
		String timeStartString = "";
		String timeStopString = "";

		public Timer( String name ) {
			this.name = name;
			this.millisStart = System.currentTimeMillis();
			this.millisStop = this.millisStart;
			this.elapsedTime = 0.0;
		}

		public Timer( String name, long millisStart ) {
			this.name = name;
			this.millisStart = millisStart;
			this.millisStop = this.millisStart;
			this.elapsedTime = 0.0;
		}

		public String getName() {
			return name;
		}

		public double getElapsedTime() {
			return elapsedTime;
		}
		
		public void updateTime() {
			this.millisStop = System.currentTimeMillis();
			this.elapsedTime = (double) (this.millisStop - this.millisStart) / 1000.0;
		}
		
		public String getTimeStartString() {
			Date date = new Date( this.millisStart );
			this.timeStartString = date.toString();
			return this.timeStartString;
		}
		public String getTimeStopString() {
			updateTime();
			Date date = new Date( this.millisStop );
			this.timeStopString = date.toString();
			return this.timeStopString;
		}
		
		public void resetTimer() {
			this.millisStart = System.currentTimeMillis();
			this.elapsedTime = 0.0;
		}
	}

	int nTimers = 0;
	LinkedHashMap< String, Timer > timers = new LinkedHashMap<>();

	public void addTimer( String name ) {
		if ( timers.containsKey(name) ) {
			IJ.log("Warning: Timer " + name + " already exists, timer will be overwritten.");
		}
		this.timers.put(name, new Timer(name));
	}

	public void addTimer( String name, long millisStart ) {
		if ( timers.containsKey(name) ) {
			IJ.log("Warning: Timer " + name + " already exists, timer will be overwritten.");
		}
		this.timers.put( name, new Timer( name, millisStart ) );
	}

	public Timer getTimer( String name ) {
		return timers.get(name);
	}
	
	public LinkedHashMap< String, String > getStringMap() {
		
		LinkedHashMap< String, String > map = new LinkedHashMap<>();

		for ( String key : this.timers.keySet() ) {
			map.put(key, Double.toString( this.timers.get(key).getElapsedTime() ) );
		}
		return map;
	}
}
