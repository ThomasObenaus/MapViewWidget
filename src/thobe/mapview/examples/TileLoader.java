/*
 * Copyright (C) 2013 ThObe. All rights reserved. Author: Thomas Obenaus EMail: thobepro@gmail.com Project: MapView
 */

package thobe.mapview.examples;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferStrategy;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import thobe.mapview.kernel.MapViewInfo;
import thobe.mapview.kernel.mapprovider.GoogleMapURLBuilder;
import thobe.mapview.kernel.mapprovider.MapProvider;
import thobe.mapview.kernel.mapprovider.MapURLBuilder;
import thobe.mapview.kernel.mapprovider.OSMStaticMapLite;
import thobe.mapview.kernel.tilesystem.GeoCoord;

/**
 * @author Thomas Obenaus
 * @source TileLoader.java
 * @date Oct 6, 2013
 */
@SuppressWarnings ( "serial")
public class TileLoader extends JFrame
{
	private static final int		IMG_SIZE		= 640;
	private static final int		READ_TIMEOUT	= 3500;
	private TileView				tileView;

	private JTextField				tf_latitude;
	private JTextField				tf_longitude;
	private JComboBox<MapProvider>	cobo_mapProvider;
	private JComboBox<Integer>		cobo_zoom;
	private GeoCoord				currentCenterOfImage;
	private JPanel					pa_buttons;

	public TileLoader()
	{
		this.setTitle( "TileLoader \t\t[" + MapViewInfo.getLibName( ) + " " + MapViewInfo.getLibVersion( ) + "]" );
		this.currentCenterOfImage = new GeoCoord( );
		this.buildGUI( );
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		this.setSize( IMG_SIZE, IMG_SIZE + 50 );

		this.loadTile( );
	}

	protected void buildGUI( )
	{
		this.setLayout( new BorderLayout( 5, 5 ) );
		this.tileView = new TileView( );
		this.add( this.tileView, BorderLayout.CENTER );

		// panel for buttons etc.
		JPanel pa_bottom = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		this.add( pa_bottom, BorderLayout.SOUTH );

		this.cobo_mapProvider = new JComboBox<>( MapProvider.values( ) );
		pa_bottom.add( cobo_mapProvider );
		this.cobo_mapProvider.setSelectedItem( MapProvider.OSMStaticMapLite );
		this.cobo_mapProvider.setToolTipText( "Mapprovider" );

		this.cobo_zoom = new JComboBox<>( new Integer[]
		{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 } );
		pa_bottom.add( cobo_zoom );
		this.cobo_zoom.setSelectedItem( 12 );
		this.cobo_zoom.setToolTipText( "Zoomlevel" );

		this.tf_latitude = new JTextField( 7 );
		pa_bottom.add( this.tf_latitude );
		this.tf_latitude.setText( currentCenterOfImage.getLatitude( ) + "" );
		this.tf_latitude.setToolTipText( "Latitude" );

		this.tf_longitude = new JTextField( 7 );
		pa_bottom.add( this.tf_longitude );
		this.tf_longitude.setText( currentCenterOfImage.getLongitude( ) + "" );
		this.tf_longitude.setToolTipText( "Longitude" );

		this.pa_buttons = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
		pa_bottom.add( pa_buttons );

		final JButton bu_loadTile = new JButton( "Load" );
		pa_buttons.add( bu_loadTile );
		bu_loadTile.setToolTipText( "Load tile from given Lat/Long" );
		bu_loadTile.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent evt )
			{
				TileLoader.this.setEnabled( false );
				loadTile( );
				TileLoader.this.setEnabled( true );
			}
		} );

		JButton bu_close = new JButton( "Close" );
		pa_buttons.add( bu_close );
		bu_close.setToolTipText( "Close" );
		bu_close.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent arg0 )
			{
				setVisible( false );
				System.exit( 0 );
			}
		} );

	}

	protected JPanel getButtonPanel( )
	{
		return pa_buttons;
	}

	public static int getImgSize( )
	{
		return IMG_SIZE;
	}

	public Canvas getTileView( )
	{
		return tileView;
	}

	private void loadTile( )
	{
		try
		{
			currentCenterOfImage = getSelectedCenter( );
		}
		catch ( NumberFormatException e )
		{
			System.err.println( e.getLocalizedMessage( ) );
			JOptionPane.showMessageDialog( this, e.getLocalizedMessage( ), "Error loding tile", JOptionPane.ERROR_MESSAGE );
			return;
		}

		URL url = null;
		try
		{
			// obtain seleceted mapprovider
			MapProvider mapProvider = ( MapProvider ) this.cobo_mapProvider.getSelectedItem( );
			MapURLBuilder urlBuilder = null;
			switch ( mapProvider )
			{
			case GOOGLE:
				urlBuilder = new GoogleMapURLBuilder( );
				break;
			case OSMStaticMapLite:
				urlBuilder = new OSMStaticMapLite( );
				break;
			case BING:
			default:
				throw new IllegalArgumentException( "The given mapprovider [" + mapProvider + "] is currently not supported" );
			}

			Integer zoom = getZoom( );

			// create the url for the tile-request
			url = urlBuilder.buildURL( currentCenterOfImage, zoom, IMG_SIZE, IMG_SIZE );

			// open the connection
			URLConnection con = url.openConnection( );
			con.setReadTimeout( READ_TIMEOUT );
			con.setConnectTimeout( READ_TIMEOUT );

			// check content length
			if ( con.getContentLengthLong( ) == 0 ) throw new IllegalArgumentException( "Loaded image is empty." );

			// read the image
			BufferedInputStream in = new BufferedInputStream( con.getInputStream( ) );
			Image tileImg = ImageIO.read( in );

			//update the view using the loaded image
			this.tileView.updateImage( tileImg );
		}
		catch ( IllegalArgumentException | IOException e )
		{
			String err = "Error loading tile from " + url + ": " + e.getLocalizedMessage( );
			System.err.println( err );
			JOptionPane.showMessageDialog( this, err, "Error loding tile", JOptionPane.ERROR_MESSAGE );
		}
	}

	public GeoCoord getCurrentCenterOfImage( )
	{
		return currentCenterOfImage;
	}

	protected Integer getZoom( )
	{
		// obtain selected zoom-level
		Integer zoom = ( Integer ) this.cobo_zoom.getSelectedItem( );
		return zoom;
	}

	protected GeoCoord getSelectedCenter( )
	{
		GeoCoord center = null;
		try
		{
			Double latitude = Double.parseDouble( this.tf_latitude.getText( ) );
			Double longitude = Double.parseDouble( this.tf_longitude.getText( ) );
			center = new GeoCoord( latitude, longitude );
		}
		catch ( NumberFormatException e )
		{
			String err = "Unable to convert given values for latitude and longitude: " + e.getLocalizedMessage( );
			throw new NumberFormatException( err );
		}
		return center;
	}

	protected void paintOverlay( Graphics2D gr )
	{
		// so nothing here, but maybe in child-classes 
	}

	@Override
	public void setEnabled( boolean b )
	{
		super.setEnabled( b );
		this.tf_latitude.setEnabled( b );
		this.tf_longitude.setEnabled( b );
	}

	public static void main( String[] args )
	{
		try
		{
			JPopupMenu.setDefaultLightWeightPopupEnabled( false );
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName( ) );
			Locale.setDefault( new Locale( "en", "GB" ) );
			TileLoader frame = new TileLoader( );
			frame.setVisible( true );
		}
		catch ( ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e )
		{
			e.printStackTrace( );
		}
	}

	public void refreshImage( )
	{
		this.tileView.repaint( );
	}

	private class TileView extends Canvas
	{
		public static final int	RENDER_QUALITY_LOW	= 0;
		public static final int	RENDER_QUALITY_HIGH	= 1;
		private BufferStrategy	strategy			= null;
		private int				renderQuality		= RENDER_QUALITY_HIGH;
		private Image			image;

		public TileView()
		{
			this.image = null;
		}

		public void updateImage( Image image )
		{
			this.image = image;
			// forece painting the new image
			this.repaint( );
		}

		@Override
		public void paint( Graphics g )
		{
			//super.paint( g );

			if ( strategy != null )
			{

				do
				{
					Graphics2D gr = ( Graphics2D ) strategy.getDrawGraphics( );

					applyRenderQuality( gr );

					gr.clearRect( 0, 0, this.getWidth( ), this.getHeight( ) );

					// draw the image
					if ( this.image != null ) gr.drawImage( image, 0, 0, null );

					TileLoader.this.paintOverlay( gr );

					gr.dispose( );
				}
				while ( strategy.contentsLost( ) || strategy.contentsRestored( ) );

				strategy.show( );
			}
		}

		@Override
		public void update( Graphics g )
		{
			paint( g );
		}

		@Override
		public void validate( )
		{
			super.validate( );
			this.createBufferStrategy( );
		}

		private void createBufferStrategy( )
		{
			if ( ( getWidth( ) > 0 ) && ( getHeight( ) > 0 ) )
			{
				this.createBufferStrategy( 2 );
				this.strategy = this.getBufferStrategy( );
			}
			else
			{
				strategy = null;
			}
		}

		/**
		 * Apply the render quality settings that were set using setRenderQuality() to the Graphics2D object given.
		 * @param gr The Graphics2D object to apply the settins to.
		 */
		private void applyRenderQuality( Graphics2D gr )
		{
			gr.setRenderingHint( RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON );
			if ( renderQuality == RENDER_QUALITY_LOW )
			{
				gr.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF );
				gr.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );
				gr.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );
				gr.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
			}
			else if ( renderQuality == RENDER_QUALITY_HIGH )
			{
				gr.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
				gr.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );
				gr.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
			}
		}

	}
}
