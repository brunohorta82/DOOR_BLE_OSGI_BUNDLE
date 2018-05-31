package pt.brunohorta.ble;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import tinyb.*;

import java.util.List;


public class Activator implements BundleActivator {
    private static final String DOOR_BLE_ADDRESS = "F5:73:02:70:37:F2";
    private BluetoothDevice sensor = null;

    class ValueNotification implements BluetoothNotification<byte[]> {
        public void run(byte[] tempRaw) {
            System.out.println(new String(tempRaw));
        }
    }

    public void start(BundleContext context) throws Exception {
        System.out.println("----------------  DOOR BLE OSGI BUNDLE STARTED ------------------");
        BluetoothManager manager = BluetoothManager.getBluetoothManager();
        manager.startDiscovery();
        sensor = getDevice();
        try {
            manager.stopDiscovery();
        } catch (BluetoothException e) {
            System.err.println("Discovery could not be stopped.");
        }

        if (sensor == null) {
            System.err.println("No sensor found with the provided address.");
            return;

        }

        System.out.print("Found device: ");
        printDevice(sensor);

        if (sensor.connect())
            System.out.println("Sensor with the provided address connected");
        else {
            System.out.println("Could not connect device.");
            return;

        }
        BluetoothGattService sensorService = getService(sensor, "6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
        if (sensorService == null) {
            System.err.println("This device does not have the door ble sensor service we are looking for.");
            return;

        }
        System.out.println("Found service " + sensorService.getUUID());


        BluetoothGattCharacteristic sensorValue = getCharacteristic(sensorService, "6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
        BluetoothGattCharacteristic sensorPeriod = getCharacteristic(sensorService, "6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
        if (sensorValue == null || sensorPeriod == null) {
            System.err.println("Could not find the correct characteristics.");
            return;

        }
        System.out.println("Found the door ble characteristics");

        sensorPeriod.enableValueNotifications(new ValueNotification());
        try {
            manager.stopDiscovery();
        } catch (BluetoothException e) {
            System.err.println("Discovery could not be stopped.");
        }

    }

    static BluetoothDevice getDevice() throws InterruptedException {
        BluetoothManager manager = BluetoothManager.getBluetoothManager();
        for (int i = 0; i < 10; i++) {
            System.out.print("..");
            System.out.println();
            List<BluetoothDevice> list = manager.getDevices();
            if (list == null)
                return null;
            for (BluetoothDevice device : list) {
                if (DOOR_BLE_ADDRESS.equals(device.getAddress())) {
                    return device;
                }
            }
            Thread.sleep(1000);
        }
        return null;
    }

    static void printDevice(BluetoothDevice device) {
        System.out.print("Address = " + device.getAddress());
        System.out.print(" Name = " + device.getName());
        System.out.print(" Connected = " + device.getConnected());
        System.out.println();
    }

    static BluetoothGattService getService(BluetoothDevice device, String UUID) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            System.out.println("Services exposed by device:");
            BluetoothGattService tempService = null;
            List<BluetoothGattService> bluetoothServices = null;

            bluetoothServices = device.getServices();
            if (bluetoothServices == null) {
                System.out.println("No Services found!");
            } else {
                for (BluetoothGattService service : bluetoothServices) {
                    System.out.println("UUID: " + service.getUUID());
                    if (service.getUUID().equalsIgnoreCase(UUID))
                        tempService = service;
                }

                return tempService;
            }
        }
        return null;
    }

    static BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, String UUID) {
        System.out.println("Characteristics exposed by service:");
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        if (characteristics == null) {
            System.out.println("No Characteristics found!");
        } else {
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                System.out.println("UUID: " + characteristic.getUUID());
                if (characteristic.getUUID().equalsIgnoreCase(UUID))
                    return characteristic;
            }
        }
        return null;
    }

    public void stop(BundleContext bc) throws Exception {
        System.out.print("Stop and disconnect");
        if (sensor != null) {
            sensor.disconnect();
        }
    }
}
