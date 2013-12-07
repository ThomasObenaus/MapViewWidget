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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import thobe.mapview.kernel.MapViewInfo;
import thobe.mapview.kernel.tilesystem.GeoCoord;
import thobe.mapview.kernel.tilesystem.MercatorProjection;

/**
 * @author Thomas Obenaus
 * @source TileLoaderWithFlag.java
 * @date Oct 13, 2013
 */
@SuppressWarnings ( "serial")
public class TileLoaderWithMarker extends TileLoader
{
	private static final Font	txtFont	= new Font( "Arial", Font.BOLD, 13 );
	private int					mouseX;
	private int					mouseY;

	public TileLoaderWithMarker( )
	{
		this.setTitle( "TileLoaderWithMarker \t\t[" + MapViewInfo.getLibName( ) + " " + MapViewInfo.getLibVersion( ) + "]" );
		this.mouseX = 0;
		this.mouseY = 0;

		this.getTileView( ).addMouseMotionListener( new MouseMotionAdapter( )
		{

			@Override
			public void mouseMoved( MouseEvent evt )
			{
				mouseX = evt.getX( );
				mouseY = evt.getY( );
				refreshImage( );
			}
		} );
	}

	public static void main( String[] args )
	{
		try
		{
			JPopupMenu.setDefaultLightWeightPopupEnabled( false );
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName( ) );
			Locale.setDefault( new Locale( "en", "GB" ) );
			TileLoaderWithMarker frame = new TileLoaderWithMarker( );
			frame.setVisible( true );
		}
		catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e )
		{
			e.printStackTrace( );
		}
	}

	@Override
	protected void buildGUI( )
	{
		super.buildGUI( );

		JPanel pa_buttons = this.getButtonPanel( );

		JButton bu_setMarker = new JButton( "Marker" );
		pa_buttons.add( bu_setMarker );
		bu_setMarker.setToolTipText( "Set the marker at the specified geo-position" );
		bu_setMarker.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				updateMarker( );
			}
		} );

	}

	protected void updateMarker( )
	{
		this.refreshImage( );
	}

	@Override
	protected void paintOverlay( Graphics2D gr )
	{
		final int outerMarkerRadius = 30;
		final int innerMarkerRadius = 6;
		final Color outerMarkerColor = new Color( 250, 0, 0, 50 );
		final Color innerMarkerColor = Color.BLUE;
		final Color hudBGColor = new Color( 220, 220, 220, 180 );

		try
		{
			GeoCoord centerOfImage = getCurrentCenterOfImage( );
			GeoCoord selectedFlagLocation = getSelectedCenter( );
			int zoom = this.getZoom( );

			// compute the pixelcoordinate on the image
			int halfImgSize = ( int ) ( getImgSize( ) / 2d );
			Point2D pixCoord = MercatorProjection.geoCoordToPixelCoordOnImage( selectedFlagLocation, centerOfImage, halfImgSize, zoom );

			double px = pixCoord.getX( );
			double py = pixCoord.getY( );

			// draw the outer marker
			gr.setColor( outerMarkerColor );
			gr.fillOval( ( int ) ( px - outerMarkerRadius ), ( int ) ( py - outerMarkerRadius ), outerMarkerRadius * 2, outerMarkerRadius * 2 );
			gr.setColor( Color.BLACK );
			gr.drawOval( ( int ) ( px - outerMarkerRadius ), ( int ) ( py - outerMarkerRadius ), outerMarkerRadius * 2, outerMarkerRadius * 2 );

			// draw the inner marker
			gr.setColor( innerMarkerColor );
			gr.fillOval( ( int ) ( px - innerMarkerRadius ), ( int ) ( py - innerMarkerRadius ), innerMarkerRadius * 2, innerMarkerRadius * 2 );

			// draw HUD background
			gr.setColor( hudBGColor );
			gr.fillRect( 3, 3, 325, 45 );
			gr.setColor( Color.GRAY );
			gr.drawRect( 3, 3, 325, 45 );

			// draw/write the geoposition as text
			gr.setColor( Color.BLACK );
			gr.setFont( txtFont );
			gr.drawString( "Marker at: " + selectedFlagLocation.getFormatted( ), ( int ) px + 5, ( int ) py - 10 );
			gr.drawString( "ImgCenter at: " + centerOfImage.getFormatted( ), 10, 20 );

			// draw mouse position
			Point2D pixCoordOfCursorOnImage = new Point2D.Double( mouseX, mouseY );
			GeoCoord underCursor = MercatorProjection.pixelCoordOnImageToGeoCoord( pixCoordOfCursorOnImage, centerOfImage, halfImgSize, zoom ); //MercatorProjection.pixelCoordOnWorldMapToGeoCoord( pixCoordOfCursorOnWorldMap, zoom );
			gr.drawString( "GeoCoord under cursor: " + underCursor.getFormatted( ), 10, 40 );

		}
		catch ( NumberFormatException e )
		{
			System.err.println( e.getLocalizedMessage( ) );
		}
	}

}
