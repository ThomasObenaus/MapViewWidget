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

import java.net.MalformedURLException;
import java.net.URL;

import thobe.mapview.kernel.tilesystem.GeoCoord;

/**
 * {@link MapURLBuilder} implementation (for OpenStreetMaps) that enables to build {@link URL}s for loading static map-images from
 * http://staticmap.openstreetmap.de/staticmap.php.
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
		query.addParameter( "markers", "color:red%%7Clabel:S%%7C51.476161,0.000595" );

		return new URL( URLBase + query );
	}

	@Override
	public MapProvider getProvider( )
	{
		return MapProvider.OSMStaticMapLite;
	}
}
