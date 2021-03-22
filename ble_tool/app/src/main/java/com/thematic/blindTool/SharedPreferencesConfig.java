package com.thematic.blindTool;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesConfig {

    private SharedPreferences sharedPreferences;
    private Context context;

    public SharedPreferencesConfig(Context applicationContext) {
        this.context = applicationContext;
        sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.ble_mac_address_sp), Context.MODE_PRIVATE);
    }

    public void SharedPreferencesConfig(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.ble_mac_address_sp), Context.MODE_PRIVATE);
    }

    public void ble_macaddress_status(String mac_address, boolean status) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getResources().getString(R.string.ble_mac_address_string), mac_address);
        editor.putBoolean(context.getResources().getString(R.string.ble_mac_address_status), status);
        editor.commit();
    }

    public boolean ble_macaddress_check() {
        boolean status = false;
        status = sharedPreferences.getBoolean(context.getResources().getString(R.string.ble_mac_address_status), status);
        return status;
    }
    public String ble_macaddress(){
        String Mac_address = null;
        Mac_address = sharedPreferences.getString(context.getResources().getString(R.string.ble_mac_address_string),Mac_address);
        return Mac_address;
    }
}
