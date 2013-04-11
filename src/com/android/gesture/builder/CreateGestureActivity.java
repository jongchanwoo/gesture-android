/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gesture.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureStroke;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class CreateGestureActivity extends Activity {
    private static final float LENGTH_THRESHOLD = 120.0f;

    private Gesture mGesture;
    private View mDoneButton;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.create_gesture);

        mDoneButton = findViewById(R.id.done);

        GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        overlay.addOnGestureListener(new GesturesProcessor());
        
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        if (mGesture != null) {
            outState.putParcelable("gesture", mGesture);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        
        mGesture = savedInstanceState.getParcelable("gesture");
        if (mGesture != null) {
            final GestureOverlayView overlay =
                    (GestureOverlayView) findViewById(R.id.gestures_overlay);
            overlay.post(new Runnable() {
                public void run() {
                    overlay.setGesture(mGesture);
                }
            });

            mDoneButton.setEnabled(true);
        }
    }

    public void sendGesture(int[] toArduino) {
    	// 1. Set up my Bluetooth
    	POEBluetoothAdaptor bluetooth = new POEBluetoothAdaptor();
        Toast.makeText(this, bluetooth.mStatus, Toast.LENGTH_LONG).show();
        
    	// 2. Set up connection with Arduino
        bluetooth.connect();
        
    	// 3. Send
        bluetooth.send(toArduino);
    }
    
    @SuppressWarnings({"UnusedDeclaration"})
    public void addGesture(View v) {
    	
        if (mGesture != null) {
        	// adding gesture layout to measure it's height and width
        	GestureOverlayView gesture_layout = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        	int height = gesture_layout.getMeasuredHeight();
        	int width = gesture_layout.getMeasuredWidth();
        	
        	
            final TextView input = (TextView) findViewById(R.id.gesture_name);
            final CharSequence name = input.getText();
            if (name.length() == 0) {
                input.setError(getString(R.string.error_missing_name));
                return;
            }

            final GestureLibrary store = GestureBuilderActivity.getStore();
            store.addGesture(name.toString(), mGesture);
            store.save();

            setResult(RESULT_OK);

            final String path = new File(Environment.getExternalStorageDirectory(),
                    "gestures").getAbsolutePath();

            
            // Initialize the array (of length 144) with all 0s.
            final int SCREEN_WIDTH = 12;
            final int SCREEN_HEIGHT = 12;
            final int ARRAY_SIZE = SCREEN_WIDTH * SCREEN_HEIGHT;
            int toArduino[] = new int[ARRAY_SIZE];
            for (int i = 0; i < ARRAY_SIZE; i++) {
            	toArduino[i] = 0;
            }
            
            // Get the array of coordinates from Gesture object
            ArrayList<GestureStroke> gestureStrokes = mGesture.getStrokes();
            GestureStroke lastStroke = gestureStrokes.get(gestureStrokes.size() - 1);
            
            // points is a list of x, y coordinates structured as follows:
            // points = {x1, y1, x2, y2, x3, y3, .....] where xi, yi is a coordinate on a screen
            
            float points[] = lastStroke.points;
            // Below is just for test.
            //int width = (int) lastStroke.boundingBox.width();
            //int height = (int) lastStroke.boundingBox.height();

            // Convert storke points to toArduino array with 144 integers
            for (int i = 0; i < points.length / 2; i++) {
            	int x_point_index = 2 * i;
            	int y_point_index = 2 * i + 1;
            	
            	int x_index = (int) (points[x_point_index] / width * SCREEN_WIDTH);
            	int y_index =  (int) (points[y_point_index] / height * SCREEN_HEIGHT);
            	
            	// Consider the edge cases
            	x_index = x_index >= SCREEN_WIDTH? SCREEN_WIDTH -1 : x_index;
            	y_index = y_index >= SCREEN_HEIGHT? SCREEN_HEIGHT -1 : y_index;
            	int index = x_index + y_index * SCREEN_WIDTH;
            	index = index < 0 ? 0 : index;
            	
        		// set the element in toArduino at 'index' to be 1
            	toArduino[index] = 1;
            } 
            
            // TO_DO: Send the toArduino Array to the Ardunio device via Bluetooth
            // toArduino designed as: each index = pin_screen_x + pin_screen_y * pin_screen_width
            // For example, if the user drew something likes this:
            // - - - - -
            //         -
            //         -
            //         -
            // then toArduino => [1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1] 
            
            Toast.makeText(this, Arrays.toString(toArduino), Toast.LENGTH_LONG).show();     
            System.out.println(toArduino);
            
            // Display that it successfully saved the trace points
            Toast.makeText(this, getString(R.string.save_success, path), Toast.LENGTH_LONG).show();
            
            // Send the Gesture to our Arduino device via Bluetooth
            sendGesture(toArduino);
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
        
    }
    
    @SuppressWarnings({"UnusedDeclaration"})
    public void cancelGesture(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }
    
    private class GesturesProcessor implements GestureOverlayView.OnGestureListener {
        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
            mDoneButton.setEnabled(false);
            mGesture = null;
        }

        public void onGesture(GestureOverlayView overlay, MotionEvent event) {
        }

        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
            mGesture = overlay.getGesture();
            if (mGesture.getLength() < LENGTH_THRESHOLD) {
                overlay.clear(false);
            }
            mDoneButton.setEnabled(true);
        }

        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        }
    }
}
