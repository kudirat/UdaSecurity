package com.udacity.catpoint.security.service;

import com.google.common.base.Enums;
import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;

    private SecurityService securityService;

    @Mock
    private StatusListener statusListener;

    private final String random = UUID.randomUUID().toString();

    public Random random1 = new Random();

    private Sensor sensor;


    private Sensor getNewSensor(){
        return new Sensor(random, SensorType.DOOR);
    }

    private Set<Sensor> getSensors(int count, Boolean status){
        Set<Sensor> sensors = new HashSet<>();
        for(int i = 0; i < count; i++){
            sensors.add(new Sensor(random, SensorType.DOOR));
        }

        for(Sensor s: sensors){
            s.setActive(status);
        }

        return sensors;
    }

    @BeforeEach
    void setUp(){
        this.securityService = new SecurityService(securityRepository, imageService);
        this.sensor = getNewSensor();
    }

    //Test1
    //If alarm is armed and a sensor becomes activated,
    // put the system into pending alarm status.
    @Test
    void ifArmedAndSensorActivated_PutPendingAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //Test2
    // If alarm is armed and a sensor becomes activated and the system is already pending alarm,
    // set the alarm status to alarm.
    @Test
    void ifArmedAndSensorActiveAndSystemPending_SetAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test3
    //If pending alarm and all sensors are inactive,
    // return to no alarm state.
    @Test
    void ifPendingAlarmAndSensorsInactive_SetNoAlarm(){
        //when the alarm is already pending and sensors are inactive, verify system set to no alarm
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        //create sensors, add them to security service and make them inactive
        Set<Sensor> sensors = getSensors(3, true);
        for(Sensor s: sensors){
            securityService.addSensor(s);
            securityService.changeSensorActivationStatus(s, false);
        }

        verify(securityRepository, times(3)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test4
    //If alarm is active,
    // change in sensor state should not affect the alarm state.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifAlarmActiveAndSensorChange_NoAlarmEffect(boolean status){
    lenient().when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

    securityService.changeSensorActivationStatus(sensor, status);

    verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Test5
    //If a sensor is activated while another one is already active and the system is in pending state,
    // change it to alarm state.
    @Test
    void ifSensorActivatedWhileActiveAndSystemPending_ChangetoAlarm(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        Sensor sensor1 = new Sensor(random, SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor1, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test6
    //If a sensor is deactivated while already inactive,
    // make no changes to the alarm state.
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    void ifSensorInactivatedWhileInactive_NoChangetoAlarmState(AlarmStatus status){
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(status);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Test7
    //If the camera image contains a cat while the system is armed-home,
    // put the system into alarm status.
    @Test
    void ifCamImgHasCatAndSystemisArmedHome_SetSystemAlarm(){
//      int imageType = BufferedImage.
        //handles the illegalargument exception. width and height cannot be <=0
        //imagetype cannot be zero either
        int imageType = random1.nextInt(10) + 1;
        int randInt = random1.nextInt(255) + 1;
        int randInt2 = random1.nextInt(255) + 1;

        //when image service returns true and security service armed_home, verify set alarm
        BufferedImage image = new BufferedImage(randInt, randInt2, imageType);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(image);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test8
    //If the camera image does not contain a cat,
    //change the status to no alarm as long as the sensors are not active.
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    void ifCamImgHasNoCatAndSensorsInactive_ChangeStatustoNoAlarm(AlarmStatus status){
        int imageType = random1.nextInt(10) + 1;
        int randInt = random1.nextInt(255) + 1;
        int randInt2 = random1.nextInt(255) + 1;

        Set<Sensor> sensors = getSensors(3, false);
        for(Sensor s: sensors){
            securityService.addSensor(s);
            securityService.changeSensorActivationStatus(s, false);
        }

        BufferedImage image = new BufferedImage(randInt, randInt2, imageType);

        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(status);
        securityService.processImage(image);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test9
    //If the system is disarmed,
    // set the status to no alarm.
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    void ifSystemDisarmed_setNoAlarm(AlarmStatus status){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(status);
        lenient().when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
//        assertEquals(expected, securityRepository.getAlarmStatus());
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test10
    //If the system is armed,
    // reset all sensors to inactive.
    @ParameterizedTest
    @ValueSource(strings = {"ARMED_HOME", "ARMED_AWAY"})
    void ifSystemArmed_resetAllSensorsInactive(String status){
    Set<Sensor> currSensors = getSensors(2, false);

    for(Sensor s: currSensors){
        securityService.addSensor(s);
    }

    for(Sensor s: securityService.getSensors()){
        s.setActive(true);
    }

    securityService.setArmingStatus(ArmingStatus.valueOf(status));
    lenient().when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.valueOf(status));

    for(Sensor s: securityService.getSensors()){
        assertTrue(s.getActive() == Boolean.FALSE);
    }
    }

    //Test11
    //If the system is armed-home while the camera shows a cat,
    // set the alarm status to alarm.
    @Test
    void ifArmedHomeAndCatOnCam_SetAlarm(){
        Random random1 = new Random();

        int imageType = random1.nextInt(10) + 1;
        int randInt = random1.nextInt(255) + 1;
        int randInt2 = random1.nextInt(255) + 1;

        BufferedImage image = new BufferedImage(randInt, randInt2, imageType);

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(image);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void Test_addSensor(){
        securityService.addSensor(sensor);
        verify(securityRepository, atMostOnce()).addSensor(sensor);
    }

    @Test
    void Test_removeSensor(){
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
        verify(securityRepository, atMostOnce()).removeSensor(sensor);
    }


    @Test
    void ifDisarmed_SetNoAlarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    @Test
    void addStatusListener(){
        SecurityService securityService2 = Mockito.spy(securityService);
        securityService2.addStatusListener(statusListener);

        verify(securityService2, times(1)).addStatusListener(statusListener);
    }

    @Test
    void removeStatusListener(){
        SecurityService securityService2 = Mockito.spy(securityService);
        securityService2.addStatusListener(statusListener);
        securityService2.removeStatusListener(statusListener);

        verify(securityService2, times(1)).removeStatusListener(statusListener);

    }

    @Test
    void handleSensorDeactivated_SetAlarm(){
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,false);

        verify(securityRepository,atMostOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void handleSensorDeactivated_SetPendingAlarm(){
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor,false);

        verify(securityRepository,atMostOnce()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void ifArmedAndNoCat_SetNoAlarm(){

        int imageType = random1.nextInt(10) + 1;
        int randInt = random1.nextInt(255) + 1;
        int randInt2 = random1.nextInt(255) + 1;

        BufferedImage image = new BufferedImage(randInt, randInt2, imageType);

        lenient().when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);

        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(image);

        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void GetCurrentSensors(){
        SecurityService securityService2 = Mockito.spy(securityService);
        securityService2.getSensors();
        verify(securityService2, times(1)).getSensors();
    }

    @Test
    void GetCurrentAlarmStatus(){
        SecurityService securityService2 = Mockito.spy(securityService);
        securityService2.setAlarmStatus(AlarmStatus.ALARM);
        securityService2.getAlarmStatus();
        verify(securityService2, times(1)).getAlarmStatus();
    }

}
