package thobe.mapview.kernel.tileloader;

public interface TileRequestListener
{
	public void onError( TileRequest tileLoader, String msg );

	public void onDone( TileRequest tileLoader );
}
