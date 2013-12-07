/*
 * Copyright (C) 2013 ThObe. All rights reserved. Author: Thomas Obenaus EMail: thobepro@gmail.com Project: MapView
 */
package thobe.mapview.kernel.mapprovider;

import java.net.MalformedURLException;
import java.net.URL;

import thobe.mapview.kernel.tilesystem.GeoCoord;

/**
 * {@link MapURLBuilder} implementation (for OpenStreetMaps) that enables to build {@link URL}s for loading static map-images from http://staticmap.openstreetmap.de/staticmap.php.
 * @author Thomas Obenaus
 */
public class OSMStaticMapLite extends OpenStreetMapURLBuilder
{
	public static final String	URLBase	= "http://staticmap.openstreetmap.de/staticmap.php";

	@Override
	public URL buildURL( GeoCoord center, int zoomLevel, int width, int height, MapType mapType ) throws MalformedURLException
	{
		URLQuery query = new URLQuery( );
		query.addParameter( "zoom", zoomLevel );
		query.addParameter( "center", center.toString( ) );
		query.addParameter( "maptype", mapType.toString( ) );
		query.addParameter( "size", width + "x" + height );
//		query.addParameter( "markers", "color:red%7Clabel:S%7C53.0,5.0" );

		return new URL( URLBase + query );
	}

	@Override
	public MapProvider getProvider( )
	{
		return MapProvider.OSMStaticMapLite;
	}
}
