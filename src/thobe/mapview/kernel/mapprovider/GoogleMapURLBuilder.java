/*
 * Copyright (C) 2013 ThObe. All rights reserved. Author: Thomas Obenaus EMail: thobepro@gmail.com Project: MapView
 */
package thobe.mapview.kernel.mapprovider;

import java.net.MalformedURLException;
import java.net.URL;

import thobe.mapview.kernel.tilesystem.GeoCoord;

/**
 * {@link MapURLBuilder} implementation (for GoogleMaps) that enables to build {@link URL}s for loading static map-images from http://maps.google.com/maps/api/staticmap.
 * @author Thomas Obenaus
 */
public class GoogleMapURLBuilder extends MapURLBuilder
{
	public static final String	URLBase	= "http://maps.google.com/maps/api/staticmap";

	@Override
	public URL buildURL( GeoCoord center, int zoomLevel, int width, int height, MapType mapType ) throws MalformedURLException
	{
		URLQuery query = new URLQuery( );
		query.addParameter( "zoom", zoomLevel );
		query.addParameter( "center", center.toString( ) );
		query.addParameter( "maptype", "roadmap" );
		query.addParameter( "size", width + "x" + height );
		query.addParameter( "sensor", false );

		return new URL( URLBase + query );
	}

	@Override
	public MapProvider getProvider( )
	{
		return MapProvider.GOOGLE;
	}
}
