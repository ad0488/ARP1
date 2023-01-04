package org.tensorflow.lite.examples.objectdetection;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import static org.tensorflow.lite.examples.objectdetection.AppConstants.PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fitpolo.support.MokoSupport;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import org.tensorflow.lite.examples.objectdetection.service.MokoService;
import org.tensorflow.lite.examples.objectdetection.utils.Utils;

import java.util.List;

public class GuideActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        requestPermission();

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (!isWriteStoragePermissionOpen()) {
                showRequestPermissionDialog();
                return;
            }
        }
        delayGotoMain();
    }

    private void requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                startActivityForResult(intent, 2296);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(GuideActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            Toast.makeText(GuideActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            Toast.makeText(GuideActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
        }


    };


    private void delayGotoMain() {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, AppConstants.REQUEST_CODE_ENABLE_BT);
            return;
        }
        if (!Utils.isLocServiceEnable(this)) {
            showOpenLocationDialog();
            return;
        }
        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (!isLocationPermissionOpen()) {
                showRequestPermissionDialog2();
                return;
            } else {
                AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                int checkOp = appOpsManager.checkOp(AppOpsManager.OPSTR_COARSE_LOCATION, Process.myUid(), getPackageName());
                if (checkOp != AppOpsManager.MODE_ALLOWED) {
                    showOpenSettingsDialog2();
                    return;
                }
            }
        }
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startService(new Intent(GuideActivity.this, MokoService.class));
                startActivity(new Intent(GuideActivity.this, BtScanActivity.class));
                GuideActivity.this.finish();
            }
        }.start();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (SDK_INT >= Build.VERSION_CODES.M) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                        boolean shouldShowRequest = shouldShowRequestPermissionRationale(permissions[0]);
                        if (shouldShowRequest) {
                            if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                                showRequestPermissionDialog2();
                            } else {
                                showRequestPermissionDialog();
                            }
                        } else {
                            if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                                showOpenSettingsDialog2();
                            } else {
                                showOpenSettingsDialog();
                            }
                        }
                    } else {
                        delayGotoMain();
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_ENABLE_BT) {
            delayGotoMain();
        }
        if (requestCode == AppConstants.REQUEST_CODE_PERMISSION) {
            if (SDK_INT >= Build.VERSION_CODES.M) {
                if (!isWriteStoragePermissionOpen()) {
                    showOpenSettingsDialog();
                } else {
                    delayGotoMain();
                }
            }
        }
        if (requestCode == AppConstants.REQUEST_CODE_PERMISSION_2) {
            delayGotoMain();
        }
        if (requestCode == AppConstants.REQUEST_CODE_LOCATION_SETTINGS) {
            if (!Utils.isLocServiceEnable(this)) {
                showOpenLocationDialog();
            } else {
                delayGotoMain();
            }
        }
    }

    private void showOpenSettingsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_storage_close_title)
                .setMessage(R.string.permission_storage_close_content)
                .setPositiveButton(getString(R.string.permission_open), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        // 根据包名打开对应的设置界面
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, AppConstants.REQUEST_CODE_PERMISSION);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                }).create();
        dialog.show();
    }

    private void showRequestPermissionDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_storage_need_title)
                .setMessage(R.string.permission_storage_need_content)
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        ActivityCompat.requestPermissions(GuideActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                }).create();
        dialog.show();
    }

    private void showOpenLocationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.location_need_title)
                .setMessage(R.string.location_need_content)
                .setPositiveButton(getString(R.string.permission_open), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, AppConstants.REQUEST_CODE_LOCATION_SETTINGS);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                }).create();
        dialog.show();
    }

    private void showOpenSettingsDialog2() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_location_close_title)
                .setMessage(R.string.permission_location_close_content)
                .setPositiveButton(getString(R.string.permission_open), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        // 根据包名打开对应的设置界面
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, AppConstants.REQUEST_CODE_PERMISSION_2);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                }).create();
        dialog.show();
    }

    private void showRequestPermissionDialog2() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_location_need_title)
                .setMessage(R.string.permission_location_need_content)
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(GuideActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                }).create();
        dialog.show();
    }
}