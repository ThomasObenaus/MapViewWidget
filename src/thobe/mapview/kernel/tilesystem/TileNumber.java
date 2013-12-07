/*
 *  Copyright (C) 2013, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    MapViewWidget
 */
package thobe.mapview.kernel.tilesystem;

/**
 * Class representing an index to address a tile (bing/osm/google tilesystem) or a coordinate within a tile. A {@link TileNumber}
 * corresponds to a {@link GeoCoord} at a specific
 * zoom-level of the map.
 * @author Thomas Obenaus
 */
public class TileNumber
{
	/**
	 * x-coordinate of the tile. This coordinate consists of the index of the tile (integer part) and the position of a geocoordinate within
	 * this tile (fractional part).
	 */
	private double	xTile;

	/**
	 * y-coordinate of the tile. This coordinate consists of the index of the tile (integer part) and the position of a geocoordinate within
	 * this tile (fractional part).
	 */
	private double	yTile;

	public TileNumber( double xTile, double yTile )
	{
		this.xTile = xTile;
		this.yTile = yTile;
	}

	public double getX( )
	{
		return xTile;
	}

	public double getY( )
	{
		return yTile;
	}

	/**
	 * Returns the x-coordinate/ index of the tile.
	 * @return
	 */
	public long getXInt( )
	{
		return ( long ) xTile;
	}

	/**
	 * Returns the Y-coordinate/ index of the tile.
	 * @return
	 */
	public long getYInt( )
	{
		return ( long ) yTile;
	}

	/**
	 * Returns the position of the {@link GeoCoord} (represented by this {@link TileNumber}) within the tile addressed by this
	 * {@link TileNumber}.
	 * @return
	 */
	public double getYFrac( )
	{
		return frac( this.getY( ) );
	}

	/**
	 * Returns the position of the {@link GeoCoord} (represented by this {@link TileNumber}) within the tile addressed by this
	 * {@link TileNumber}.
	 * @return
	 */
	public double getXFrac( )
	{
		return frac( this.getX( ) );
	}

	/**
	 * Computes the fractional part of the given value
	 * @param value
	 * @return
	 */
	private static double frac( double value )
	{
		double integerPart = ( long ) value;
		// the fracional part
		return ( value - integerPart );
	}

	@Override
	public String toString( )
	{
		return "[" + getXInt( ) + "," + getYInt( ) + "]";
	}
}
