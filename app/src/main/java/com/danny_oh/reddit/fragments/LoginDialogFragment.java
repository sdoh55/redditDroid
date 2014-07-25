package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.danny_oh.reddit.R;

/**
 * Created by danny on 7/24/14.
 *
 * DialogFragment that displays the login dialog
 */
public class LoginDialogFragment extends DialogFragment {

    public interface LoginDialogListener {
        public void onLoginClick(String username, String password);
    }

    private LoginDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (LoginDialogListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement the LoginDialogListener interface");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        final View dialogView = inflater.inflate(R.layout.dialog_login, null);

        builder.setView(dialogView)
                .setPositiveButton(R.string.button_login, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String username = ((TextView)dialogView.findViewById(R.id.username)).getText().toString();
                        String password = ((TextView)dialogView.findViewById(R.id.password)).getText().toString();

                        mListener.onLoginClick(username, password);
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        LoginDialogFragment.this.dismiss();
                    }
                });

        return builder.create();
    }


}
