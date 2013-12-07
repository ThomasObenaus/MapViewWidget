/*
 * Copyright (C) 2013 ThObe. All rights reserved. Author: Thomas Obenaus EMail: thobepro@gmail.com Project: MapView
 */
package thobe.mapview.kernel.mapprovider;

/**
 * Enum representing different map-types (satellite, roadmap,...)
 * @author Thomas Obenaus
 */
public enum MapType
{
	ROADMAP, SATELLITE, HYBRID, TERRAIN;

	public String toString( )
	{
		switch ( this )
		{
		case ROADMAP:
			return "roadmap";
		case SATELLITE:
			return "sattelite";
		case HYBRID:
			return "hybrid";
		case TERRAIN:
			return "terrain";
		default:
			return "roadmap";
		}
	}
}
