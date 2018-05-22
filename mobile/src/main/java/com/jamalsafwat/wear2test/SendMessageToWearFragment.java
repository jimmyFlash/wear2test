package com.jamalsafwat.wear2test;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * A placeholder fragment containing a simple view.
 */
public class SendMessageToWearFragment extends Fragment {

    private static SendMessageCallBack sndMsgCallback;
    private EditText te;
    private Button btn;

    private Context cntxt;
    private Button btn2;


    public interface SendMessageCallBack {

        /**
         * Called when the playback of the audio file ends. This should be called on the UI thread.
         */
        void sendMessageToWear(String strBytes);
        void sendAnotherMessageToWear(String strBytes);
    }

    public SendMessageToWearFragment() {
    }

    public static SendMessageToWearFragment newInstance(SendMessageCallBack sndMsg) {

        Bundle args = new Bundle();

        SendMessageToWearFragment fragment = new SendMessageToWearFragment();
        fragment.setArguments(args);

        sndMsgCallback = sndMsg;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_send_message_to_wear, container, false);

        te = (EditText) v.findViewById(R.id.textView);

        btn = (Button) v.findViewById(R.id.button3);
        btn2 = (Button) v.findViewById(R.id.button4);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sndMsgCallback.sendMessageToWear(te.getText().toString());
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sndMsgCallback.sendAnotherMessageToWear(te.getText().toString() + " LOLOLOLOLO");
            }
        });


        cntxt = getActivity();

        return v;
    }
}
