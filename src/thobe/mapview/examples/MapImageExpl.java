/*
 * Copyright (C) 2013 ThObe. All rights reserved. Author: Thomas Obenaus EMail: thobepro@gmail.com Project: MapView
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
	private Log			log;

	public MapImageExpl()
	{

		this.setTitle( "MapImage \t\t[" + MapViewInfo.getLibName( ) + " " + MapViewInfo.getLibVersion( ) + "]" );
		this.setSize( 1280, 980 );
		this.buildGUI( );
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		this.addComponentListener( new ComponentAdapter( )
		{

			@Override
			public void componentResized( ComponentEvent cEv )
			{
				if ( mapImage != null )
				{
					mapImage.setViewPort( getWidth( ), getHeight( ) );
				}
			}
		} );
	}

	private void buildGUI( )
	{
		this.setLayout( new BorderLayout( 0, 0 ) );
		this.mapImage = new MapImage( 800, 600, Log.LOG( ) );
		this.add( this.mapImage, BorderLayout.CENTER );
	}

	public static void main( String[] args )
	{
		try
		{
			MapImage.setDebug( true );
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
