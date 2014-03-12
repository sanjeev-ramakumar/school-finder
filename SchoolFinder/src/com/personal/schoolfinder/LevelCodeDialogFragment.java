package com.personal.schoolfinder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.DialogFragment;

public class LevelCodeDialogFragment extends DialogFragment {
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.select_school)
				.setItems(R.array.school_array, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
						SharedPreferences.Editor editor = sharedPref.edit();
						switch (which) {
						case 1:
							editor.putString("level_code", "elementary-schools");
							break;
						case 2:
							editor.putString("level_code", "middle-schools");
							break;
						case 3:
							editor.putString("level_code", "high-schools");
							break;
						default:
							editor.remove("level_code");
						}
						editor.commit();
						
			            FragmentManager fragmentManager = getFragmentManager();
			            Fragment fragment = fragmentManager.findFragmentById(R.id.content_frame);
			            if (fragment instanceof MapsFragment) {
			            	fragment.onResume();
			            }

					}
		});
		return builder.create();
	}
	
}
