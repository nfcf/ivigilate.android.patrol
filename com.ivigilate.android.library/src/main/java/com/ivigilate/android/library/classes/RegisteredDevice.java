package com.ivigilate.android.library.classes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Returned by the server after registering
 */
public class RegisteredDevice {

    private String id;
    private String name;
    private int battery;
    private boolean is_active;
    private String uid;
    private String type;
    private DeviceProvisioning.DeviceType deviceType;
    private String metadata;

    public RegisteredDevice(){}

    public RegisteredDevice(String name, DeviceProvisioning.DeviceType deviceType, int battery, boolean isActive, String uid, String metadata){
        this.name = name;
        this.deviceType = deviceType;
        this.battery = battery;
        this.is_active = isActive;
        this.uid = uid;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public boolean isActive() {
        return is_active;
    }

    public void setIsActive(boolean isActive) {
        this.is_active = isActive;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DeviceProvisioning.DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceProvisioning.DeviceType deviceType) {
        this.deviceType = deviceType;
    }
    public JsonObject getMetadata() {
        JsonParser parser = new JsonParser();
        return parser.parse(this.metadata).getAsJsonObject();
    }
}
