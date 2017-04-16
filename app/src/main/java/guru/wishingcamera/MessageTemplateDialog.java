package guru.wishingcamera;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 */
public class MessageTemplateDialog extends DialogFragment {
//
    CharSequence[] templates = {"Congrats for the new couple","Have a happy married life "};
    public MessageTemplateDialog() {
        // Required empty public constructor
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.message_array, new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            CharSequence msg = "which item:" + which + "   :" + getResources().getStringArray(R.array.message_array)[which];
                            Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT ).show();
                        }
                    });

        // Create the AlertDialog object and return it
        return builder.create();
    }


}
