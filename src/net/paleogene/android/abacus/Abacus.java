package net.paleogene.android.abacus;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class Abacus extends Activity {
    // TODO Pass the TextView's ID as a custom attribute to the AbacusView
    // or something
    public static TextView readout;
    private AbacusView av;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle state) {
        Log.v("Abacus", "onCreate()");
        
        super.onCreate(state);
        setContentView(R.layout.main);
        
        readout = (TextView)   findViewById(R.id.readout);
        av      = (AbacusView) findViewById(R.id.abacus);
    }
    
    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        av.saveState(state);
    }
}