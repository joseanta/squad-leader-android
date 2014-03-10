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
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.squadleader.R;
import com.esri.squadleader.controller.MapController;

/**
 * A dialog for letting the user input an MGRS location and navigating to it.
 */
public class DimensionPantallaDialogFragment extends DialogFragment {
    
    /**
     * A listener for this class to pass objects back to the Activity that called it.
     */
    public interface DimensionPantallaHelper {
        
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
        void beforePanToMgrs(String mgrs);
        
        /**
         * Called if the attempt to pan to MGRS is unsuccessful. Most often, this happens
         * when the provided MGRS string is invalid.
         * @param mgrs the MGRS string, which is likely invalid.
         */
        void onPanToMgrsError(String mgrs);
    }
    
    private DimensionPantallaHelper listener = null;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DimensionPantallaHelper) {
            listener = (DimensionPantallaHelper) activity;
        }
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (null != listener) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final View inflatedView = inflater.inflate(R.layout.go_to_mgrs, null);
            builder.setView(inflatedView);
            builder.setTitle(getString(R.string.dimension_pantalla));
            
            final RadioGroup rg = (RadioGroup)inflatedView.findViewById(R.id.radioGroup1);
            //rg.setVisibility(View.INVISIBLE);
            final RadioButton rb1= (RadioButton)inflatedView.findViewById(R.id.radioButton1);
            final RadioButton rb2= (RadioButton)inflatedView.findViewById(R.id.radioButton2);
            final RadioButton rb3= (RadioButton)inflatedView.findViewById(R.id.radioButton3);
            TextView txt = (TextView)inflatedView.findViewById(R.id.textView_mgrsLabel);
            rb1.setText("Rifle");
            rb2.setText("Morter");
            rb3.setText("Artillery");
            txt.setText("Introducir Tamaño de la Ventana de Visión");
            final EditText etxt = (EditText)inflatedView.findViewById(R.id.editText_mgrs);
            
            rg.setOnCheckedChangeListener(new OnCheckedChangeListener() 
            {
            	public void onCheckedChanged(RadioGroup group, int checkedId) 
                {
                 // TODO Auto-generated method stub
            		if(rb1.isChecked())
            			etxt.setText("800");
            		if(rb2.isChecked())
            			etxt.setText("2000");
            		if(rb3.isChecked())
            			etxt.setText("15000");           		
                }
            });
            
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
            	public void onClick(DialogInterface diolog, int which) {
            		listener.getMapController().DibujarDimension(false);
            	}
            });
            
            builder.setPositiveButton(R.string.dimension_pantalla, new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int which) {
                    View view = inflatedView.findViewById(R.id.editText_mgrs);
                    if (null != view && view instanceof EditText) {
                        String mgrs = ((EditText) view).getText().toString();
                        if (null != mgrs) {
                            //listener.beforePanToMgrs(mgrs);
                            //if (null == listener.getMapController().panTo(mgrs)) {
                            if (null == listener.getMapController().panToDimension(mgrs)) { 
                                Toast.makeText(getActivity(), "Dimensión Invalida: " + mgrs, Toast.LENGTH_LONG).show();
                                listener.onPanToMgrsError(mgrs);
                            }
                        }
                    }
                }
            });
            return builder.create();
        } else {
            return null;
        }
    }

}
