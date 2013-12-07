/*
 * Copyright (C) 2013 ThObe. All rights reserved. Author: Thomas Obenaus EMail: thobepro@gmail.com Project: MapView
 */
package thobe.mapview.kernel.tilesystem;

/**
 * Class representing a geocoordinate (lat,long).
 * @author Thomas Obenaus
 */
public class GeoCoord
{
	private double	latitude;
	private double	longitude;

	/**
	 * Default Ctor, located at greenwich
	 */
	public GeoCoord()
	{
		// greenwich
		this( 51.477222d, 0d );
	}

	/**
	 * Ctor
	 * @param latitude - latitude in degree
	 * @param longitude - longitude in degree
	 */
	public GeoCoord( double latitude, double longitude )
	{
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * The latitude in degree.
	 * @return
	 */
	public double getLatitude( )
	{
		return latitude;
	}

	/**
	 * The latitude in degree
	 * @return
	 */
	public double getLongitude( )
	{
		return longitude;
	}

	public String getFormatted( )
	{
		return String.format( "%.5f | %.5f", latitude, longitude );
	}

	@Override
	public String toString( )
	{
		return latitude + "," + longitude;
	}

	/**
	 * Subtracts the given {@link GeoCoord} from this {@link GeoCoord}-instance.
	 * @param geoCoord
	 * @return - result = this - geoCoord
	 */
	public GeoCoord subtract( GeoCoord geoCoord )
	{
		return new GeoCoord( this.getLatitude( ) - geoCoord.getLatitude( ), this.getLongitude( ) - geoCoord.getLongitude( ) );
	}

	/**
	 * Adds the given {@link GeoCoord} to this {@link GeoCoord}-instance.
	 * @param geoCoord
	 * @return - result = this + geoCoord
	 */
	public GeoCoord add( GeoCoord geoCoord )
	{
		return new GeoCoord( this.getLatitude( ) + geoCoord.getLatitude( ), this.getLongitude( ) + geoCoord.getLongitude( ) );
	}

	public GeoCoord abs( )
	{
		return new GeoCoord( Math.abs( this.getLatitude( ) ), Math.abs( this.getLongitude( ) ) );
	}
}
