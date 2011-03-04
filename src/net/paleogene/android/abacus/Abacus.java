package net.paleogene.android.abacus;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class Abacus extends Activity {
    // TODO Pass the TextView's ID as a custom attribute to the AbacusView
    // or something
    public static TextView readout;
    public static AbacusView av;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        Log.v("Abacus", "onCreate()");
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        readout = (TextView)   findViewById(R.id.readout);
        av      = (AbacusView) findViewById(R.id.abacus);
    }
    
    @Override
    public void onStart() {
        Log.v("Abacus", "onStart()");
        super.onStart();
    }
    
    @Override
    public void onPause() {
        Log.v("Abacus", "onPause()");
        super.onStart();
    }
    
    @Override
    public void onStop() {
        Log.v("Abacus", "onStop()");
        super.onStart();
    }
    
    @Override
    public void onDestroy() {
        Log.v("Abacus", "onDestroy()");
        super.onStart();
    }
    
    @Override
    public void onRestart() {
        Log.v("Abacus", "onRestart()");
        super.onStart();
    }
    
    @Override
    public void onResume() {
        Log.v("Abacus", "onResume()");
        super.onStart();
    }
}