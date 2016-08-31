package com.ivigilate.android.patrol.classes;

import com.ivigilate.android.library.classes.BleDeviceSighting;
import com.ivigilate.android.library.classes.Sighting;
import com.ivigilate.android.library.interfaces.IDeviceSighting;
import com.ivigilate.android.library.interfaces.ISighting;

/**
 * Created by joanaPeixoto on 24-Jun-16.
 * Creates a common ground between all types of Sightings
 * so methods can be accessed as necessary
 */
public class SightingEx implements ISighting {

    private ISighting mSighting;


    public SightingEx(ISighting sighting) {
        mSighting = sighting;
    }

    @Override
    public String getUUID() {
        return mSighting.getUUID();
    }

    @Override
    public String getType() {
        return mSighting.getType();
    }

    public String getMac() {
        String mac;
        return mac = mSighting instanceof BleDeviceSighting ? ((BleDeviceSighting) mSighting).getMac() :
                null;
    }

    public String getName() {
        String name;
        return name = mSighting instanceof BleDeviceSighting ? ((BleDeviceSighting) mSighting).getName() :
                null;
    }

    public String getData() {
        String data;
        return data = mSighting instanceof BleDeviceSighting ? ((BleDeviceSighting) mSighting).getData() :
                null;
    }

    public Sighting.Status getStatus() {
        Sighting.Status status;
        return status = mSighting instanceof IDeviceSighting ? ((IDeviceSighting) mSighting).getStatus() :
                null;
    }
}
