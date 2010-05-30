package arb.mportal;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsoluteLayout;
import android.widget.TextView;
import arb.mportal.models.POI;
import arb.mportal.util.BoundingBox;
import arb.mportal.util.IEach;
import arb.mportal.util.L;
import arb.mportal.views.DefaultPOIView;
import arb.services.PoiServiceFH;



@SuppressWarnings("deprecation")  
public class MainActivity extends Activity implements LocationReceivable {
	
	private boolean initial = true; 
	private Location currentLocation = null;
	private LocationManager lm = null;
	private LocationListenerImpl locationListener = null;
	private Camera camera = null; 
	private AbsoluteLayout contentView = null;
	private BroadcastReceiver poiBroadcastReceiver = null;
	public static TextView t = null;  
	private static Handler handler = new Handler();
	private static Runnable r = new Runnable() {
		public void run() {
			calc();
		}
	}; 
	private static float zRot = 0.0f;
	
	
    public void onCreate(Bundle icicle) { 
    	
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);  

        int h = ViewGroup.LayoutParams.FILL_PARENT; 
        int w = ViewGroup.LayoutParams.WRAP_CONTENT;
        
        contentView = new AbsoluteLayout(this);  
         
        ArbSurface s = new ArbSurface(this);
        s.setCreationCallbacks(this); 
        contentView.addView(s, h, w); 

        t = new TextView(this); 
        //t.setText("hallo"); 
        contentView.addView(t, new ViewGroup.LayoutParams(h, w));        

        //setContentView(R.layout.main);
        setContentView(contentView); 
        
        // daheim: 47.768924832344055 12.081044912338257 
        
        //ArbSurface surface = (ArbSurface)findViewById(R.id.surface); 
        //surface.setCreationCallbacks(this); 
 
        locationListener = new LocationListenerImpl(this);  
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);  
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 1, locationListener);

        poiBroadcastReceiver = new BroadcastReceiver() {
			@Override 
			public void onReceive(Context context, Intent intent) {
				poiListReceived();
			}
		}; 
		registerReceiver(poiBroadcastReceiver, new IntentFilter(PoiServiceFH.POI_LIST_LOADED)); 
    }
    
    
    private static void calc() {
    	handler.post(new Runnable() {
			public void run() {
				POI.eachPoi(new IEach() {
					public void each(Object item, int index) {
						POI p = (POI)item; 
						AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams)p.getView().getLayoutParams();
						lp.x = (int)zRot; 
						p.getView().setLayoutParams(lp);
					}
				});
			}
		}); 
    }
    

    private void poiListReceived() { 
        draw(); 
        unregisterReceiver(poiBroadcastReceiver); 
        // final TextView view = (TextView)findViewById(R.id.myLocationText);
        SensorManager sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE); 
        SensorEventListener listener = new SensorEventListener() {
			public void onSensorChanged(SensorEvent e) {
				zRot = e.values[0]; 
				new Thread(r).start(); 
			}
			public void onAccuracyChanged(Sensor arg0, int arg1) {
				;
			}
		}; 
        sm.registerListener(listener, sm.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);      	
    } 
    
    
    
    @SuppressWarnings("deprecation") 
	private void draw() {
    	POI.eachPoi(new IEach() { 
			public void each(Object item, int index) {
				POI p = (POI)item;
	    		p.setDistance(currentLocation.distanceTo(p.getLocation()));
	    		DefaultPOIView t = new DefaultPOIView(MainActivity.this, p);   
	    		p.setView(t);  
	    		AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(170, 42, 0, 80 * index);
	    		contentView.addView(t, lp);				
			}
		});
    }


    public void receiveNewLocation(Location loc) {
    	currentLocation = loc;

    	if(initial) {
    		initial = false;
	        TextView t = (TextView)findViewById(R.id.myLocationText);
	        //String str = "Pos: " + loc.getLatitude() + " / " + loc.getLongitude();
	        // t.setText(str); 
	        
	        // 48.3617902 // 10.9086766
	
	        // 10-28 00:29:25.404: INFO/test(10990): 48.361505866666676 - 10.906298216666668

	        
	        //L.i(loc.getLatitude() + " - " + loc.getLongitude()); 
	        //System.exit(-1);
	
	        //loc.setLatitude(10.906298216666668); 
	        //loc.setLongitude(48.361505866666676);
	        BoundingBox bb = new BoundingBox(loc, 0.3); 
	        // String params = bb.urlEncode();
	        Intent serviceIntent = new Intent(this, PoiServiceFH.class); 
	        serviceIntent.putExtra("params", bb.urlEncode()); 
	        startService(serviceIntent); 
	        
	        System.out.println("");
	        //lm.removeUpdates(locationListener);
    	}
    }
    
    
    public void surfaceCreated(SurfaceHolder holder) {
    	camera = Camera.open(); 
    	try { 
	    	camera.setPreviewDisplay(holder);  
	    	camera.startPreview();  
    	} catch(Exception e) {
    		Log.d("CAMERA", e.getMessage()); 
    	}
    }
    
    
    public void surfaceDestroyed(SurfaceHolder holder) {
    	camera.stopPreview();
    	camera.release();
    }

}