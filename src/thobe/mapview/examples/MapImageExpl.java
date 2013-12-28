/*
 *  Copyright (C) 2013, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    MapViewWidget
 */
package thobe.mapview.examples;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.logging.Level;

import javax.swing.JFrame;

import thobe.mapview.kernel.MapImage;
import thobe.mapview.kernel.MapViewInfo;
import thobe.tools.log.Log;

/**
 * @author Thomas Obenaus
 * @source MapImageExpl.java
 * @date Nov 24, 2013
 */
public class MapImageExpl extends JFrame
{
	private MapImage	mapImage;

	public MapImageExpl( )
	{

		this.setTitle( "MapImage \t\t[" + MapViewInfo.getLibName( ) + " " + MapViewInfo.getLibVersion( ) + "]" );
		this.setSize( 1100, 800 );
		this.buildGUI( );
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		this.addComponentListener( new ComponentAdapter( )
		{

			@Override
			public void componentResized( ComponentEvent cEv )
			{
				if ( mapImage != null )
				{
					//mapImage.setViewPort( getWidth( ), getHeight( ) );
				}
			}
		} );
	}

	private void buildGUI( )
	{
		this.setLayout( new BorderLayout( 0, 0 ) );
		this.mapImage = new MapImage( getWidth( ), getHeight( ), Log.LOG( ) );
		this.add( this.mapImage, BorderLayout.CENTER );
	}

	public static void main( String[] args )
	{
		try
		{
			Log.initLog( "MapImageExample", Level.INFO );
			MapImageExpl frame = new MapImageExpl( );
			frame.setVisible( true );
		}
		catch ( SecurityException | IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace( );
		}
	}
}
