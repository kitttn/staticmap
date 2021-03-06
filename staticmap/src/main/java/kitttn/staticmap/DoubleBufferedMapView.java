package kitttn.staticmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.support.percent.PercentRelativeLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author kitttn
 */

public class DoubleBufferedMapView extends PercentRelativeLayout {
    private static final double ONE_DEGREE_IN_M = 111320;
    private static final double MAX_ZOOM = 21.0;
    private static final String TAG = "DoubleBufferedMapView";
    private static final String MAPS_KEY = "AIzaSyBXpnndL6qbx9Ii-Dc73gLGs5icLQBjQow";
    private static final String MAPS_URL = "http://maps.google.com/maps/api/staticmap?center=%s,%s"
            + "&scale=2&zoom=%s&size=640x640&sensor=false&key=" + MAPS_KEY;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    private double lat = 0;
    private double lng = 0;
    private long triggerTime = 0;
    private int clicks = 0;
    private Context context;
    private ImageView primaryBuffer;
    private ImageView overlay;
    private ProgressBar progress;
    private MyTarget target = new MyTarget();

    private List<Integer> zoomLevels = new ArrayList<>();
    private double multiplier = 0;
    private int currentZoomPointer = 0;
    private int offset = 0;
    private Random random;

    public DoubleBufferedMapView(Context context) {
        this(context, null);
    }

    public DoubleBufferedMapView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.layout, this, true);

        primaryBuffer = (ImageView) getChildAt(0);
        overlay = (ImageView) getChildAt(1);
        progress = (ProgressBar) getChildAt(2);

        init();
    }

    public void setZoomLevels(int... levels) {
        zoomLevels.clear();
        for (int level : levels) zoomLevels.add(level);
        if (levels.length > 0) {
            multiplier = .6 / (levels[levels.length - 1] - levels[0]);
        }
    }

    public void setSeed(long seed) {
        random = new Random(seed);
    }

    public void setLatLngOffset(double lat, double lng, int offsetInMeters) {
        this.lat = lat;
        this.lng = lng;
        this.offset = offsetInMeters;
        setRandomLocationOffset(offsetInMeters);
    }

    public void loadMap() {
        String url = String.format(MAPS_URL, lat, lng, getNextZoomLevel());
        progress.setVisibility(VISIBLE);
        Picasso.with(context)
                .load(url)
                .into(target);
    }

    private void init() {
        this.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                loadMap();
            }
        });
    }

    private void setRandomLocationOffset(int meters) {
        if (random == null)
            random = new Random();
        // at first, let's check longitude
        if (Math.abs(lng) <= 0.0000001)
            throw new Error("Need to set location first!");
        // okay, now calculate one degree in your longitude
        double ourDegree = ONE_DEGREE_IN_M * Math.cos(lng * Math.PI / 180);
        // good, going forward, calculate degrees based on radius you provided
        double offset = meters * 2.0 / ourDegree;
        lng += 2 * random.nextFloat() * offset - offset;
    }

    private int getNextZoomLevel() {
        if (zoomLevels.size() == 0)
            throw new Error("Zoom levels not set!");

        currentZoomPointer = (currentZoomPointer + 1) % zoomLevels.size();
        int lvl = zoomLevels.get(currentZoomPointer);
        return lvl;
    }

    private Bitmap getResizedBitmap(Bitmap bm, float scale) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        // CREATE A MATRIX FOR THE MANIPULATION

        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scale, scale);
        // RECREATE THE NEW BITMAP

        Bitmap bitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return bitmap;
    }

    private void calculatePercent(int currentZoom) {
        float percent = (float) (.1 + multiplier * (currentZoom - zoomLevels.get(0)));
        Log.i(TAG, "calculatePercent: percent: " + percent * 100);

        PercentRelativeLayout.LayoutParams params = (LayoutParams) overlay.getLayoutParams();
        params.getPercentLayoutInfo().heightPercent = percent;
        params.getPercentLayoutInfo().widthPercent = percent;
        overlay.setLayoutParams(params);
    }

    private Bitmap transform(Bitmap source) {
        source.compress(Bitmap.CompressFormat.JPEG, 80, out);
        //source.recycle();
        Bitmap result = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));
        out.reset();
        return result;
    }

    private class MyTarget implements Target {
        @Override public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            primaryBuffer.setImageBitmap(transform(bitmap));
            progress.setVisibility(GONE);
            calculatePercent(zoomLevels.get(currentZoomPointer));
        }

        @Override public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    }
}
