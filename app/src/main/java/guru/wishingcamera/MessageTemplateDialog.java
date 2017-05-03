package guru.wishingcamera;


import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;


/**
 * A simple {@link Fragment} subclass.
 */
public class MessageTemplateDialog extends AppCompatDialogFragment {

    public interface MessageTemplateListener {
        public void onMessageTemplateClick(DialogInterface dialogFragment, String whichMessage, int which);
    }

    MessageTemplateListener messageTemplateListener=null;

    public MessageTemplateDialog() {
        // Required empty public constructor
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.message_array, new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            String message = getResources().getStringArray(R.array.message_array)[which];
                            messageTemplateListener.onMessageTemplateClick(dialog, message, which);
                        }
                    });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            messageTemplateListener = (MessageTemplateListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

}
