package com.capstonebau.opencvapplication;

import android.os.ParcelUuid;

public class BluetoothEraserDevice {
    private String device_name;
    private String device_address;
    private int device_bond_state;
    private int device_type;
    private ParcelUuid[] device_uuids;

    public BluetoothEraserDevice() {
        // constructor
    }

    public BluetoothEraserDevice(String device_name, String device_address, int device_bond_state, int device_type, ParcelUuid[] device_uuids) {
        this.device_name = device_name;
        this.device_address = device_address;
        this.device_bond_state = device_bond_state;
        this.device_type = device_type;
        this.device_uuids = device_uuids;
    }

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public String getDevice_address() {
        return device_address;
    }

    public void setDevice_address(String device_address) {
        this.device_address = device_address;
    }

    public int getDevice_bond_state() {
        return device_bond_state;
    }

    public void setDevice_bond_state(int device_bond_state) {
        this.device_bond_state = device_bond_state;
    }

    public int getDevice_type() {
        return device_type;
    }

    public void setDevice_type(int device_type) {
        this.device_type = device_type;
    }

    public ParcelUuid[] getDevice_uuids() {
        return device_uuids;
    }

    public void setDevice_uuids(ParcelUuid[] device_uuids) {
        this.device_uuids = device_uuids;
    }
}
