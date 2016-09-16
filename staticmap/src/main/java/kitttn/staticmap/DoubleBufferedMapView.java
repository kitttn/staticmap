package kitttn.staticmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kitttn
 */

public class DoubleBufferedMapView extends RelativeLayout {
    private static final double ONE_DEGREE_IN_M = 111320;
    private static final String TAG = "DoubleBufferedMapView";
    private static final String MAPS_KEY = "AIzaSyBXpnndL6qbx9Ii-Dc73gLGs5icLQBjQow";
    private static final String MAPS_URL = "http://maps.google.com/maps/api/staticmap?center=%s,%s"
            + "&scale=2&zoom=%s&size=640x640&sensor=false&key=" + MAPS_KEY;

    private double lat = 0;
    private double lng = 0;
    private long triggerTime = 0;
    private int clicks = 0;
    private Context context;
    private ImageView primaryBuffer;
    private ImageView secondaryBuffer;
    private Bitmap lastLoadedBitmap = null;

    private List<Integer> zoomLevels = new ArrayList<>();
    private int currentZoomPointer = 0;

    public DoubleBufferedMapView(Context context) {
        super(context, null);
    }

    public DoubleBufferedMapView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.layout, this, true);

        primaryBuffer = (ImageView) getChildAt(1);
        secondaryBuffer = (ImageView) getChildAt(0);

        init();
    }

    public void setZoomLevels(int... levels) {
        zoomLevels.clear();
        for (int level : levels) zoomLevels.add(level);
    }

    public void setLatLngOffset(double lat, double lng, int offsetInMeters) {
        this.lat = lat;
        this.lng = lng;
        setRandomLocationOffset(offsetInMeters);
    }

    public void loadMap() {
        String url = String.format(MAPS_URL, lat, lng, getNextZoomLevel());
        if (lastLoadedBitmap != null)
            primaryBuffer.setImageBitmap(getResizedBitmap(lastLoadedBitmap, 1.5f));

        Picasso.with(context).load(url).into(secondaryBuffer, new Callback.EmptyCallback() {
            @Override public void onSuccess() {
                Bitmap bmp = ((BitmapDrawable) secondaryBuffer.getDrawable()).getBitmap();
                lastLoadedBitmap = bmp;
                primaryBuffer.setImageBitmap(bmp);
            }
        });
    }

    private void init() {
        this.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                loadMap();
            }
        });
    }

    private void setRandomLocationOffset(int meters) {
        // at first, let's check longitude
        if (lng <= 0.0000001)
            throw new Error("Need to set location first!");
        // okay, now calculate one degree in your longitude
        double ourDegree = ONE_DEGREE_IN_M * Math.cos(lng * Math.PI / 180);
        // good, going forward, calculate degrees based on radius you provided
        double offset = meters * 2.0 / ourDegree;
        lng += 2 * Math.random() * offset - offset;
    }

    private int getNextZoomLevel() {
        if (zoomLevels.size() == 0)
            throw new Error("Zoom levels not set!");

        currentZoomPointer = (currentZoomPointer + 1) % zoomLevels.size();
        return zoomLevels.get(currentZoomPointer);
    }

    private Bitmap getResizedBitmap(Bitmap bm, float scale) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        // CREATE A MATRIX FOR THE MANIPULATION

        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scale, scale);
        // RECREATE THE NEW BITMAP

        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }
}
