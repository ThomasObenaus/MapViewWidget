/*
 *  Copyright (C) 2013, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    MapViewWidget
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
