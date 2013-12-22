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

import java.awt.Color;

import thobe.mapview.kernel.tilesystem.GeoCoord;

/**
 * @author Thomas Obenaus
 * @source Marker.java
 * @date Dec 22, 2013
 */
public class Marker
{
	private Color		color;
	private GeoCoord	position;
	private Character	label;

	public Marker( GeoCoord position, Character label, Color color )
	{
		this.position = position;
		this.label = label;
		this.color = color;
	}

	public Marker( GeoCoord position, Character label )
	{
		this( position, label, Color.red );
	}

	public Marker( GeoCoord position )
	{
		this( position, 'A', Color.red );
	}

	public Character getLabel( )
	{
		return label;
	}

	public Color getColor( )
	{
		return color;
	}

	public GeoCoord getPosition( )
	{
		return position;
	}

	public String getColorHex( )
	{
		return colorToHexColor( this.color );
	}

	public static String colorToHexColor( Color color )
	{
		return "0x" + String.format( "%02X%02X%02X", color.getRed( ), color.getGreen( ), color.getBlue( ) );
	}
}
