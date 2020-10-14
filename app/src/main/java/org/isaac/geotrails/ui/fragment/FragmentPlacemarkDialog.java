package org.isaac.geotrails.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.greenrobot.eventbus.EventBus;
import org.isaac.geotrails.GeoTrailsApplication;
import org.isaac.geotrails.R;
import org.isaac.geotrails.eventbus.EventBusMSG;

public class FragmentPlacemarkDialog extends DialogFragment {

    EditText DescEditText;

    //@SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder createPlacemarkAlert = new AlertDialog.Builder(getActivity(), R.style.StyledDialog);
        createPlacemarkAlert.setTitle(R.string.dlg_add_placemark);
        Drawable icon = getResources().getDrawable(R.mipmap.ic_add_location_white_24dp);

        // Set the right icon color, basing on the day/night theme active
        switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
            case Configuration.UI_MODE_NIGHT_NO:
                // Night mode is not active, we're in day time
                final float[] NEGATIVE = {
                        -1.0f, 0, 0, 0, 255, // red
                        0, -1.0f, 0, 0, 255, // green
                        0, 0, -1.0f, 0, 255, // blue
                        0, 0, 0, 1.00f, 0  // alpha
                };
                icon.setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                // Night mode is active, we're at night!
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                // We don't know what mode we're in, assume notnight
                break;
        }
        createPlacemarkAlert.setIcon(icon);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = (View) inflater.inflate(R.layout.fragment_placemark_dialog, null);

        DescEditText = (EditText) view.findViewById(R.id.placemark_description);
        DescEditText.postDelayed(new Runnable() {
            public void run() {
                if (isAdded()) {
                    DescEditText.requestFocus();
                    InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.showSoftInput(DescEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 200);

        createPlacemarkAlert.setView(view)
                .setPositiveButton(R.string.dlg_button_add, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (isAdded()) {
                            String PlacemarkDescription = DescEditText.getText().toString();
                            final GeoTrailsApplication GlobalVariables = (GeoTrailsApplication) getActivity().getApplicationContext();
//                            GlobalVariables.setPlacemarkDescription(PlacemarkDescription.trim());
                            EventBus.getDefault().post(EventBusMSG.ADD_PLACEMARK);
                            //Log.w("myApp", "[#] FragmentPlacemarkDialog.java - posted ADD_PLACEMARK: " + PlacemarkDescription);
                        }
                    }
                })
                .setNegativeButton(R.string.dlg_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return createPlacemarkAlert.create();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}