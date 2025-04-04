package com.example.ignc;

import android.content.ComponentName;
import android.content.Context;

import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.usdk.apiservice.aidl.DeviceServiceData;
import com.usdk.apiservice.aidl.UDeviceService;
import com.usdk.apiservice.aidl.beeper.UBeeper;
import com.usdk.apiservice.aidl.device.UDeviceManager;
import com.usdk.apiservice.aidl.system.USystem;
import com.usdk.apiservice.limited.DeviceServiceLimited;
import com.usdk.apiservice.aidl.printer.UPrinter;
import com.usdk.apiservice.aidl.system.location.ULocation;
import com.usdk.apiservice.aidl.device.DeviceInfo;



public final class DeviceHelper implements ServiceConnection {
    private static final String TAG = "DeviceHelper";
    private static DeviceHelper me = new DeviceHelper();
    private volatile boolean isBinded = false;
    private UDeviceService deviceService;
    private Context context;

    public void init(Context context) {
        this.context = context;
    }

    private int retry = 0;

    public static DeviceHelper me() {
        return me;
    }

    public void bindService() {
        if (isBinded) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction("com.usdk.apiservice");
        intent.setPackage("com.usdk.apiservice");
        context.bindService(intent, me, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "SDK service connected.");
        retry = 0;
        isBinded = true;
        deviceService = UDeviceService.Stub.asInterface(service);

        try {
            register(true);

        } catch (IllegalStateException e) {
            Log.e(TAG, "Registro falhou", e);
        }

        DeviceServiceLimited.bind(context, deviceService, new DeviceServiceLimited.ServiceBindListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "DeviceServiceLimited vinculado com sucesso.");
            }

            @Override
            public void onFail() {
                Log.e(TAG, "Falha ao vincular DeviceServiceLimited.");
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "Serviço desconectado.");
        try {
            unregister();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Falha no unregister", e);
        }
        deviceService = null;
        isBinded = false;
        DeviceServiceLimited.unbind(context);
        bindService();
    }

    public void register(boolean useEpayModule) throws IllegalStateException {
        try {
            Bundle param = new Bundle();
            param.putBoolean(DeviceServiceData.USE_EPAY_MODULE, useEpayModule);
            deviceService.register(param, new Binder());
        } catch (RemoteException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
    }

    public void unregister() throws IllegalStateException {
        try {
            deviceService.unregister(null);
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
    }

    public UBeeper getBeeper() throws IllegalStateException {
        IBinder iBinder = new IBinderCreator() {
            @Override
            IBinder create() throws RemoteException {
                return deviceService.getBeeper();
            }
        }.start();
        return UBeeper.Stub.asInterface(iBinder);
    }

    public USystem getSystem() throws IllegalStateException {
        IBinder iBinder = new IBinderCreator() {
            @Override
            IBinder create() throws RemoteException {
                return deviceService.getSystem();
            }
        }.start();
        return USystem.Stub.asInterface(iBinder);
    }

    public ULocation getLocation() throws IllegalStateException {
        IBinder iBinder = new IBinderCreator() {
            @Override
            IBinder create() throws RemoteException {
                return getSystem().getLocation();
            }
        }.start();
        return ULocation.Stub.asInterface(iBinder);
    }

    public UDeviceManager getDeviceManager() throws IllegalStateException {
        IBinder iBinder = new IBinderCreator() {
            @Override
            IBinder create() throws RemoteException {
                return deviceService.getDeviceManager();
            }
        }.start();
        return UDeviceManager.Stub.asInterface(iBinder);
    }




    public UPrinter getPrinter() throws IllegalStateException {
        IBinder iBinder = new IBinderCreator() {
            @Override
            IBinder create() throws RemoteException {
                return deviceService.getPrinter();
            }
        }.start();
        return UPrinter.Stub.asInterface(iBinder);
    }


    abstract class IBinderCreator {
        IBinder start() throws IllegalStateException {
            if (deviceService == null) {
                bindService();
                throw new IllegalStateException("Servic unbound,please retry latter!");
            }
            try {
                return create();

            } catch (DeadObjectException e) {
                deviceService = null;
                throw new IllegalStateException("Service process has stopped,please retry latter!");

            } catch (RemoteException | SecurityException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }

        abstract IBinder create() throws RemoteException;
    }

}