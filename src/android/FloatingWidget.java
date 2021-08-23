package com.emersonar.plugin.widgetfloat;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class FloatingWidget extends CordovaPlugin {

    private static final int DRAW_OVER_OTHER_APP_PERMISSION = 4321;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private CallbackContext callbackContextPermission = null;
    private final int CODE_REQUEST_PERMISSION = 1001;

    @Override
    public boolean execute(String action, JSONArray args,
                           CallbackContext callbackContext) throws JSONException {

        if (action.equals("open")) {
            openFloatingWidget();
            startObserver(args.getJSONObject(0));
            /// getPermissionLocationService(args.getJSONObject(0));
            return true;
        }

        if (action.equals("askPermissionLocation")) {
            callbackContextPermission = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    askPermissionLocation();
                }
            });
            return true;
        }

        if (action.equals("checkSystemOverlayPermission")) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("status", checkSystemOverlayPermission());
                callbackContext.success(jsonObject);
            } catch (JSONException e) {
                callbackContext.error("Ocorreu um erro ao obter status");
            }
            return true;
        }

        if (action.equals("getPermissionLocation")) {
            getPermissionLocation(callbackContext);
            return true;
        }

        if (action.equals("getPermission")) {
            askForSystemOverlayPermission();
            return true;
        }

        if (action.equals("close")) {
            closeFloatingWidget();
            return true;
        }

        return false; // Returning false results in a "MethodNotFound" error.
    }

    private boolean checkSystemOverlayPermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(cordova.getContext()));
    }

    private void openFloatingWidget() {
        Activity context = cordova.getActivity();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            context.startService(new Intent(cordova.getActivity(), FloatingWidgetService.class));
            // finish();
        } else if (Settings.canDrawOverlays(cordova.getActivity())) {
            context.startService(new Intent(cordova.getActivity(), FloatingWidgetService.class));
            // finish();
        } else {
            askForSystemOverlayPermission();
            Toast.makeText(cordova.getActivity(), "You need System Alert Window Permission to do this", Toast.LENGTH_SHORT).show();
        }

    }

    private void askForSystemOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(cordova.getContext())) {

            //If the draw over permission is not available to open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + cordova.getContext().getPackageName()));
            cordova.getActivity().startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION);
        }
    }

    private void getPermissionLocation(CallbackContext callbackContext) {
        boolean permissionAccessCoarseLocationApproved =
                ActivityCompat.checkSelfPermission(cordova.getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        if (permissionAccessCoarseLocationApproved) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                boolean backgroundLocationPermissionApproved =
                        ActivityCompat.checkSelfPermission(cordova.getContext(),
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                == PackageManager.PERMISSION_GRANTED;

                if (!backgroundLocationPermissionApproved) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("isPermissionBackground", true);
                        jsonObject.put("message", "Apenas habilitado em primeiro plano");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    callbackContext.error(jsonObject);
                } else {
                    callbackContext.success();
                }
                return;
            }
            callbackContext.success();
        }

        callbackContext.error("Não possui permissão");

    }

    private void askPermissionLocation() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(cordova.getActivity(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("WoosmapGeofencing", "Displaying permission rationale to provide additional context.");
            ActivityCompat.requestPermissions(cordova.getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    CODE_REQUEST_PERMISSION);
        } else {
            Log.i("WoosmapGeofencing", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(cordova.getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    CODE_REQUEST_PERMISSION);
        }
    }

    private void closeFloatingWidget() {
        Intent intent = new Intent(cordova.getContext(), FloatingWidgetService.class);
        cordova.getContext().stopService(intent);
    }


    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);

        if (requestCode == CODE_REQUEST_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (callbackContextPermission != null) {
                    callbackContextPermission.success();
                }
            } else {
                if (callbackContextPermission != null) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("isPermissionBackground", false);
                    jsonObject.put("message", "Permissão negada");
                    this.callbackContextPermission.success(jsonObject);
                }
            }
        }
    }


    private void getPermissionLocationService(JSONObject object) {
        Dexter
                .withActivity(cordova.getActivity())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        updateLocation(object);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }

    private void updateLocation(JSONObject object) {

        buildLocationRequest();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(cordova.getActivity());
        if (ActivityCompat.checkSelfPermission(cordova.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(cordova.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, getPendingIntent(object));
    }

    private PendingIntent getPendingIntent(JSONObject object) {
        Intent intent = new Intent(cordova.getContext(), LocationService.class);
        intent.setAction(LocationService.ACTION_PROCESS_UPDATE);
       /* try {
            intent.putExtra("url", object.getString("url"));
            intent.putExtra("driverId", object.getInt("driverId"));
            intent.putExtra("userId", object.getInt("userId"));
            intent.putExtra("token", object.getString("token"));
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        return PendingIntent.getBroadcast(cordova.getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void buildLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
    }

    private void startObserver(JSONObject object) throws JSONException {
        FirebaseApp firebaseApp = FirebaseApp.initializeApp(cordova.getContext());
        FirebaseFirestore db = FirebaseFirestore.getInstance(firebaseApp);
        CollectionReference citiesRef = db.collection("trips");
        citiesRef
                .whereArrayContains("callingDriver", object.getString("driverId"))
                .whereEqualTo("driverId", null)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null)
                            return;

                        Log.d("Firestore", "test");

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED: {

                                    Log.d("Firestore", "Dados: " + dc.getDocument().getData().toString());
                                    Intent launchIntent = cordova.getActivity().getPackageManager()
                                            .getLaunchIntentForPackage(cordova.getActivity().getPackageName());
                                    if (launchIntent != null) {
                                        cordova.getActivity().startActivity(launchIntent);
                                    }
                                }
                                case REMOVED:
                                    Log.d("Firestore", "Dados: " + dc.getDocument().getData());
                                    break;
                            }
                        }
                    }
                });
    }
}