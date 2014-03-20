/*******************************************************************************
 * Copyright 2013 Esri
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.esri.squadleader.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.core.geometry.Point;
import com.esri.squadleader.R;
import com.esri.squadleader.controller.MapController;

/**
 * A dialog for letting the user input an MGRS location and navigating to it.
 */
public class BufferPantallaDialogFragment extends DialogFragment {
    
    /**
     * A listener for this class to pass objects back to the Activity that called it.
     */
    public interface BufferPantallaHelper {
        
        /**
         * Gives GoToMgrsDialogFragment a pointer to the MapController.
         * @return the application's MapController.
         */
        public MapController getMapController();
        
        /**
         * Called when GoToMgrsDialog is about to perform the pan to the MGRS location.
         * This is useful for disabling Follow Me, for example.
         * @param mgrs the MGRS string.
         */
        //void beforePanToMgrs(String mgrs);
        
        /**
         * Called if the attempt to pan to MGRS is unsuccessful. Most often, this happens
         * when the provided MGRS string is invalid.
         * @param mgrs the MGRS string, which is likely invalid.
         */
        //void onPanToMgrsError(String mgrs);
    }
    
    private BufferPantallaHelper listener = null;
    
    public Point puntoManual;
    public Point puntoGPS;
    public Point puntoElegido;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof BufferPantallaHelper) {
            listener = (BufferPantallaHelper) activity;
        }
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (null != listener) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final View inflatedView = inflater.inflate(R.layout.buscar_cercanos, null);
            builder.setView(inflatedView);
            builder.setTitle("Buscar elementos");
            
            final RadioGroup rg = (RadioGroup)inflatedView.findViewById(R.id.radioGroup1);
            final RadioButton rb1= (RadioButton)inflatedView.findViewById(R.id.radioButton1);
            final RadioButton rb2= (RadioButton)inflatedView.findViewById(R.id.radioButton2);
            final TextView txt_lon = (TextView)inflatedView.findViewById(R.id.text_longitud);
            txt_lon.setText("Lon = " + Double.toString(puntoManual.getX()));
            final TextView txt_lat = (TextView)inflatedView.findViewById(R.id.text_latitud);
            txt_lat.setText("Lat = " + Double.toString(puntoManual.getY()));
            final EditText etxt = (EditText)inflatedView.findViewById(R.id.editText_distancia);
            etxt.setText("1000");
            
            puntoElegido = puntoManual;
            rg.setOnCheckedChangeListener(new OnCheckedChangeListener() 
            {
            	public void onCheckedChanged(RadioGroup group, int checkedId) 
                {
                 // TODO Auto-generated method stub
            		if(rb1.isChecked()){
                        txt_lon.setText("Lon = " + Double.toString(puntoManual.getX()));
                    	txt_lat.setText("Lat = " + Double.toString(puntoManual.getY()));
                    	puntoElegido = puntoManual;
            		}
            		if(rb2.isChecked() && puntoGPS != null){
                        txt_lon.setText("Lon = " + Double.toString(puntoGPS.getX()));
                        txt_lat.setText("Lat = " + Double.toString(puntoGPS.getY()));
                        puntoElegido = puntoGPS;
            		}
                }
            });
            
            SeekBar bufferDistance = (SeekBar) inflatedView.findViewById(R.id.distancia_buscar);
            //bufferDistance.setMax(5000);
            bufferDistance.setProgress(1000);
    		bufferDistance.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					etxt.setText(Integer.toString(progress));
				}
			});

            
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
            	public void onClick(DialogInterface diolog, int which) {
            		//listener.getMapController().DibujarDimension(false);
            	}
            });
            
            builder.setPositiveButton("Buscar", new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int which) {
                    //View view = inflatedView.findViewById(R.id.editText_mgrs);
                    //if (null != view && view instanceof EditText) {
                        String distanciaStr = ((EditText) inflatedView.findViewById(R.id.editText_distancia)).getText().toString();
                        if (null != distanciaStr) {
                        	Double distancia = Double.parseDouble(distanciaStr);
                        	if (distancia >0){
                        		listener.getMapController().BuscarElementos(puntoElegido, distancia);
                            }
                        }
                        else 
                        	return;
                    //}
                }
            });
            return builder.create();
        } else {
            return null;
        }
    }

}
