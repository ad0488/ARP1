package org.tensorflow.lite.examples.objectdetection;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;

import com.fitpolo.support.MokoSupport;

import org.tensorflow.lite.examples.objectdetection.service.MokoService;

public class myMainActivity extends BaseActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        checkBluetoothConnection();
    }


    private void checkBluetoothConnection() {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, AppConstants.REQUEST_CODE_ENABLE_BT);
            return;
        }
        startNextActivity();

    }

    private void startNextActivity()
    {
        //start the service MokoService
        startService(new Intent(this, MokoService.class));
        //start the Activity  BtScanActivity
        startActivity(new Intent(this, BtScanActivity.class));

        myMainActivity.this.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppConstants.REQUEST_CODE_ENABLE_BT) {
            startNextActivity();
        }


    }

}
