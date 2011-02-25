package net.paleogene.android.abacus;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class Abacus extends Activity {
    // TODO Pass the TextView's ID as a custom attribute to the AbacusView
    // or something
    public static TextView readout;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        readout = (TextView) findViewById(R.id.readout);
    }
}