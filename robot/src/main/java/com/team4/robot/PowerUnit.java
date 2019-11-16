package com.team4.robot;

import com.team4.commons.*;
import com.team4.sensor.FloorDao;
import com.team4.sensor.SensorSimulator;

import static com.team4.commons.State.LOW_BATTERY;
import static com.team4.commons.State.OFF;

class PowerUnit implements PowerManager {

    private double batteryLevel;

    PowerUnit() {
        int maxBatteryLevel = Integer.parseInt(ConfigManager.getConfiguration("maxBatteryLevel"));
        setBatteryLevel(maxBatteryLevel);
    }

    @Override
    public void recharge() {
        int timeToCharge = Integer.parseInt(ConfigManager.getConfiguration("timeToCharge"));
        Utilities.doLoopedTimeDelay("Charging", timeToCharge, RobotCleanSweep.getInstance().getZeroTime());
        int maxBatteryLevel = Integer.parseInt(ConfigManager.getConfiguration("maxBatteryLevel"));
        setBatteryLevel(maxBatteryLevel);
    }

    @Override
    public double getBatteryLevel() {
        return batteryLevel;
    }

    @Override
    public void updateBatteryLevel(double units) {
        if(units < 0 || units > 3) {
            throw new RobotException("Invalid power usage level.");
        }
        setBatteryLevel(getBatteryLevel() - units);

        double batteryNeededToReachToKnownChargingStation;
        if(RobotCleanSweep.getInstance().getChargingStations().size() > 0) {
        	 batteryNeededToReachToKnownChargingStation = 200;
        } else {
        	batteryNeededToReachToKnownChargingStation = 0;
        }
        
        if(getBatteryLevel() <= batteryNeededToReachToKnownChargingStation) {
            for(Location chargingStation : RobotCleanSweep.getInstance().getChargingStations()) {
            	AStar aStar = new AStar(RobotCleanSweep.getInstance().getGraph(),RobotCleanSweep.getInstance().getLocation(),chargingStation ,2);
            	if(aStar.search()!=null) {
                	double temp = aStar.getPathNode().getMaxFloorCost() + 7.0;
                	
                	if(temp <= batteryNeededToReachToKnownChargingStation) {
                		batteryNeededToReachToKnownChargingStation = temp;
                		RobotCleanSweep.getInstance().setCurrentChargingStation(chargingStation);
                	}
            	} else {
            		if(batteryNeededToReachToKnownChargingStation == 200) {
            			batteryNeededToReachToKnownChargingStation = 0;
            		}
            	}
            }
        }
        
        if(getBatteryLevel() <= batteryNeededToReachToKnownChargingStation) {
        	String dirtLevel = Integer.toString(RobotCleanSweep.getInstance().getVacuumCleaner().getDirtLevel());
        	LogManager.logForUnity(RobotCleanSweep.getInstance().getLocation(), "GO_CHARGE", Double.toString(getBatteryLevel()), dirtLevel, RobotCleanSweep.getNumberOfRuns());
            if(RobotCleanSweep.getInstance().getState() != LOW_BATTERY) {
                RobotCleanSweep.getInstance().setState(LOW_BATTERY);
            }
            FloorDao floorDao = SensorSimulator.getInstance().getLocationInfo(RobotCleanSweep.getInstance().getLocation());
            RobotCleanSweep.getInstance().logTileInfo(
                    floorDao,
                    floorDao,
                    RobotCleanSweep.getInstance().getPowerManager().getBatteryLevel(),
                    RobotCleanSweep.getInstance().getVacuumCleaner().getDirtLevel(),
                    RobotCleanSweep.getInstance().getVacuumCleaner().getDirtLevel(),
                    null,
                    WorkingMode.DEPLOYED);
        	//LogManager.print("Going back to charging station. Location: " + RobotCleanSweep.getInstance().getLocation() + "  Battery Level: " + getBatteryLevel(), RobotCleanSweep.getInstance().getZeroTime());
        }
    }

    private void setBatteryLevel(double batteryLevel) {
        final int maxBatteryLevel = Integer.parseInt(ConfigManager.getConfiguration("maxBatteryLevel"));
        if( batteryLevel > maxBatteryLevel) {
            throw new RobotException("Invalid battery level.");
        }
        if(batteryLevel < 0) {
        	RobotCleanSweep.getInstance().setState(OFF);
        	return;
        }
        this.batteryLevel = batteryLevel;
    }
}
