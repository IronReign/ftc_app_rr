/*
Copyright (c) 2016 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESSFOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.firstinspires.ftc.teamcode;

import android.view.View;

import com.qualcomm.ftccommon.SoundPlayer;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.vuforia.HINT;
import com.vuforia.PIXEL_FORMAT;
import com.vuforia.Vuforia;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import java.util.Locale;

import static org.firstinspires.ftc.teamcode.Pose.servoNormalize;

/**
 * This file contains an minimal example of a Linear "OpMode". An OpMode is a 'program' that runs in either
 * the tertiaryAuto or the teleop period of an FTC match. The names of OpModes appear on the menu
 * of the FTC Driver Station. When an selection is made from the menu, the corresponding OpMode
 * class is instantiated on the Robot Controller and executed.
 *
 * This particular OpMode just executes a basic Tank Drive Teleop for a PushBot
 * It includes all the skeletal structure that all linear OpModes contain.
 *
 * Use Android Studios to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list
 */

@TeleOp(name="Game_6832", group="Linear Opmode")  // @Autonomous(...) is the other common choice
//  @Autonomous

public class Game_6832 extends LinearOpMode {

    //hi im testing something

    /* Declare OpMode members. */
    private ElapsedTime runtime = new ElapsedTime();

    private Pose robot = new Pose();

    SoundPlayer deadShotSays = SoundPlayer.getInstance();

    private boolean active = true;
    boolean joystickDriveStarted = false;
    private int state = 0;
    private boolean isBlue = false;


    private boolean liftDeposit = false;
    private boolean liftVerticalDeposit = false;
    private boolean liftHome = false;
    private boolean liftCollect = false;

    //drive train control variables
    private double pwrDamper = 1;
    private double pwrFwd = 0;
    private double pwrStf = 0;
    private double pwrRot = 0;
    private double pwrFwdL = 0;
    private double pwrStfL = 0;
    private double pwrFwdR = 0;
    private double pwrStfR = 0;
    private double beaterDamper = .75;
    private boolean enableTank = false;
    private long damperTimer = 0;


    //staging and timer variables
    private int autoStage = 0;
    private int autoSetupStage = 0;
    private  int vuTestMode = 0;
    private long autoTimer = 0;
    private long autoDelay = 0;
    public int codexFlashStage = 0;
    public long codexFlashTimer = 0;


    //vision objects/vision-based variables
    public VuforiaTrackables relicCodex;
    public int savedVuMarkCodex = 0;
    VuforiaTrackable relicTemplate;
    VuforiaLocalizer locale;
    private ColorBlobDetector mDetector;
    private int beaconConfig = 0;
    private double vuPwr = 0;
    public boolean vuFlashDemo = false;


    //sensors/sensing-related variables
    Orientation angles;
    boolean jewelMatches = false;


    //these are meant as short term testing variables, don't expect their usage
    //to be consistent across development sessions
    double testableDouble = robot.kpDrive;
    double testableHeading = 0;
    boolean testableDirection = true;


    //values associated with the buttons in the toggleAllowed method
    private boolean[] buttonSavedStates = new boolean[11];
    private int a = 0; //lower glyph lift
    private int b = 1; //toggle grip/release on glyph
    private int x = 2; //no function
    private int y = 3; //raise glyph lift
    private int dpad_down = 4; //glyph lift bottom position
    private int dpad_up = 5; //glyph lift top position
    private int dpad_left = 6; //no function
    private int dpad_right = 7; //glyph lift mid position
    private int left_bumper = 8; //increment state down (always)
    private int right_bumper = 9; //increment state up (always)
    private int startBtn = 10; //toggle active (always)




    @Override
    public void runOpMode() throws InterruptedException {

        robot.init(this.hardwareMap, isBlue);

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        configureDashboard();

        VuforiaLocalizer.Parameters params = new VuforiaLocalizer.Parameters(R.id.cameraMonitorViewId);
        params.vuforiaLicenseKey = RC.VUFORIA_LICENSE_KEY;
        params.cameraDirection = VuforiaLocalizer.CameraDirection.FRONT;

        locale = ClassFactory.createVuforiaLocalizer(params);
        locale.setFrameQueueCapacity(1);
        Vuforia.setFrameFormat(PIXEL_FORMAT.RGB565, true);

        Vuforia.setHint (HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 1);

        //set vuforia to look for assets from the Relic Recovery library
        relicCodex = locale.loadTrackablesFromAsset("RelicVuMark");
        relicCodex.get(0).setName("RelicTemplate");

        robot.glyphSystem.closeGrip();
        robot.jewel.hitRight();


        relicTemplate = relicCodex.get(0);

//        waitForStart(); //this is commented out but left here to document that we are still doing the functions that waitForStart() normally does, but needed to customize it.

        //activate vuforia to start identifying targets/vuMarks
        relicCodex.activate();
        robot.resetMotors(true);

        mDetector = new ColorBlobDetector();

        while(!isStarted()){    // Wait for the game to start (driver presses PLAY)

                if(isBlue)
                {
                    robot.ledSystem.bluePos();
                    autoSetupStage++;
                }
                else
                {
                    robot.ledSystem.redPos();
                    autoSetupStage++;
                }

            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }


            if (gamepad1.dpad_up) {
                robot.glyphSystem.tiltPhoneUp();
                robot.glyphSystem.raiseLift2();
            } else if (gamepad1.dpad_down) {
                robot.glyphSystem.tiltPhoneUp();
                robot.glyphSystem.lowerLift2();
            } else {
                robot.glyphSystem.stopBelt();
            }

            stateSwitch();

            if(toggleAllowed(gamepad1.x,x)) {

                    isBlue = !isBlue;

            }
            if(toggleAllowed(gamepad1.a,a)){

                autoDelay--;
                if(autoDelay < 0) autoDelay = 15;

            }
            if(toggleAllowed(gamepad1.y, y)){

                autoDelay++;
                if(autoDelay>15) autoDelay = 0;

            }

            if(toggleAllowed(gamepad1.dpad_left, dpad_left)){
                robot.glyphSystem.resetLift();
            }

            telemetry.addData("Status", "Initialized");
            telemetry.addData("Status", "Auto Delay: " + Long.toString(autoDelay) + "seconds");
            telemetry.addData("Status", "Side: " + getAlliance());
            telemetry.update();

            idle(); // Always call idle() at the bottom of your while(opModeIsActive()) loop
        }




//        robot.jewel.liftArm();
        runtime.reset();


        // run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {
            telemetry.update();
            stateSwitch();
            if(active) {
                switch(state){
                    case 0: //code for tele-op control
                        joystickDrive();
                        break;
                    case 1: //this is the tertiaryAuto we use if our teamates can also go for the beacons more reliably than we can; scores 2 balls and pushes the cap ball, also parks on the center element
                        autonomous();
                        break;
                    case 2:
                        autonomous2();
                        break;
                    case 3:
                        testLED();
                        break;
                    case 4:
                        demo((VuforiaTrackableDefaultListener) relicTemplate.getListener(),500);

                        break;
                    case 5: //provides data for forwards/backwards calibration
                        joystickDriveStarted = false;
                        if(robot.driveForward(false, 1, .5)) active = false;
                        break;
                    case 6: //provides data for left/right calibration
                        joystickDriveStarted = false;
                        if(robot.getAverageAbsTicks() < 2000){
                            robot.driveMixerMec(0,0,0);
                        }
                        else robot.driveMixerMec(0,0,0);
                        break;
                    case 7: //IMU demo mode
                        if(robot.jewel.retractArm())
                            active = false;
                        break;
                    case 8: //servo testing mode
                        robot.servoTester(toggleAllowed(gamepad1.dpad_up, dpad_up), toggleAllowed(gamepad1.y, y), toggleAllowed(gamepad1.a,a), toggleAllowed(gamepad1.dpad_down, dpad_down));
                        break;
                    case 9:
                        if(robot.jewel.extendArm())
                            active = false;
                        break;
                    default:
                        robot.stopAll();
                        break;
                }
                robot.updateSensors();
            }
            else {
                robot.ledSystem.offPos();
                robot.stopAll();
            }

            idle(); // Always call idle() at the bottom of your while(opModeIsActive()) loop
        }
    }

    public void testLED(){
        if(gamepad1.a){
            robot.ledSystem.pinkPos();
        }else if(gamepad1.b) {
            robot.ledSystem.redPos();
        }else if(gamepad1.x){
            robot.ledSystem.bluePos();
        }else if(gamepad1.y){
            robot.ledSystem.aquaPos();
        }else if(gamepad1.right_bumper){
            robot.ledSystem.offPos();
        }
    }



    public void demo(VuforiaTrackableDefaultListener beaconTarget, double distance){
        if(gamepad1.x){
            robot.maintainHeading(gamepad1.x);
        }
        else {


        }
        if(toggleAllowed(gamepad1.y, y)) {
            savedVuMarkCodex = getRelicCodex();
            vuFlashDemo = true;
        }
        if(vuFlashDemo){
            if(flashRelicCodex()){
                vuFlashDemo = false;
            }
        }
    }



    public String getRelicCodexStr(){
        RelicRecoveryVuMark relicConfig = RelicRecoveryVuMark.from(relicTemplate);
        if(relicConfig != RelicRecoveryVuMark.UNKNOWN){
            if(relicConfig == RelicRecoveryVuMark.LEFT) return "left";
            else if(relicConfig == RelicRecoveryVuMark.RIGHT) return "right";
            else return "center";
        }
        return "unknown";
    }

    public int getRelicCodex(){
        RelicRecoveryVuMark relicConfig = RelicRecoveryVuMark.from(relicTemplate);
        if(relicConfig != RelicRecoveryVuMark.UNKNOWN){
            if(relicConfig == RelicRecoveryVuMark.LEFT) return 0;
            else if(relicConfig == RelicRecoveryVuMark.RIGHT) return 2;
            else return 1;
        }
        return 1;
    }

    public boolean flashRelicCodex(){
        switch (savedVuMarkCodex){
            case 0:
                switch (codexFlashStage){
                    case 0:
                        codexFlashTimer = futureTime(.5f);
                        robot.headLampOff();
                        codexFlashStage++;
                        break;
                    case 1:
                        if(codexFlashTimer < System.nanoTime()) {
                            codexFlashTimer = futureTime(.15f);
                            robot.headLampOn();
                            codexFlashStage++;
                        }
                        break;
                    case 2:
                        if(codexFlashTimer < System.nanoTime()){
                            robot.headLamp.setPower(0);
                            codexFlashTimer = futureTime(.5f);
                            codexFlashStage++;}
                        break;
                    case 3:
                        if(codexFlashTimer < System.nanoTime()){
                            robot.headLampOn();
                            codexFlashStage = 0;
                            return true;
                        }
                        break;
                    default:
                        codexFlashStage = 0;
                        break;
                }
                break;
            case 1:
                switch (codexFlashStage){
                    case 0:
                        codexFlashTimer = futureTime(.5f);
                        robot.headLamp.setPower(0);
                        codexFlashStage++;
                        break;
                    case 1:
                        if(codexFlashTimer < System.nanoTime()) {
                            codexFlashTimer = futureTime(.15f);
                            robot.headLampOn();
                            codexFlashStage++;
                        }
                        break;
                    case 2:
                        if(codexFlashTimer < System.nanoTime()){
                            robot.headLamp.setPower(0);
                            codexFlashTimer = futureTime(.15f);
                            codexFlashStage++;}
                        break;
                    case 3:
                        if(codexFlashTimer < System.nanoTime()) {
                            codexFlashTimer = futureTime(.15f);
                            robot.headLampOn();
                            codexFlashStage++;
                        }
                        break;
                    case 4:
                        if(codexFlashTimer < System.nanoTime()){
                            robot.headLamp.setPower(0);
                            codexFlashTimer = futureTime(.5f);
                            codexFlashStage++;}
                        break;
                    case 5:
                        if(codexFlashTimer < System.nanoTime()){
                            robot.headLampOn();
                            codexFlashStage = 0;
                            return true;
                        }
                        break;
                    default:
                        codexFlashStage = 0;
                        break;
                }
                break;
            case 2:
                switch (codexFlashStage){
                    case 0:
                        codexFlashTimer = futureTime(.5f);
                        robot.headLamp.setPower(0);
                        codexFlashStage++;
                        break;
                    case 1:
                        if(codexFlashTimer < System.nanoTime()) {
                            codexFlashTimer = futureTime(.15f);
                            robot.headLampOn();
                            codexFlashStage++;
                        }
                        break;
                    case 2:
                        if(codexFlashTimer < System.nanoTime()){
                            robot.headLamp.setPower(0);
                            codexFlashTimer = futureTime(.15f);
                            codexFlashStage++;}
                        break;
                    case 3:
                        if(codexFlashTimer < System.nanoTime()) {
                            codexFlashTimer = futureTime(.15f);
                            robot.headLampOn();
                            codexFlashStage++;
                        }
                        break;
                    case 4:
                        if(codexFlashTimer < System.nanoTime()){
                            robot.headLamp.setPower(0);
                            codexFlashTimer = futureTime(.15f);
                            codexFlashStage++;}
                        break;
                    case 5:
                        if(codexFlashTimer < System.nanoTime()) {
                            codexFlashTimer = futureTime(.15f);
                            robot.headLampOn();
                            codexFlashStage++;
                        }
                        break;
                    case 6:
                        if(codexFlashTimer < System.nanoTime()){
                            robot.headLamp.setPower(0);
                            codexFlashTimer = futureTime(.5f);
                            codexFlashStage++;}
                        break;
                    case 7:
                        if(codexFlashTimer < System.nanoTime()){
                            robot.headLampOn();
                            codexFlashStage = 0;
                            return true;
                        }
                        break;
                    default:
                        codexFlashStage = 0;
                        break;
                }
                break;

        }
        return false;
    }




    public void joystickDrive(){

        /*button indexes:
        0  = a
        1  = b
        2  = x
        3  = y
        4  = dpad_down
        5  = dpad_up
        6  = dpad_left
        7  = dpad_right
        8  = left bumper
        9  = right bumper
        10 = start button
        */

        if (!joystickDriveStarted) {
            robot.resetMotors(true);
            joystickDriveStarted = true;
        }

        if(robot.glyphSystem.roll < 345 && robot.glyphSystem.roll > 180)
            robot.glyphSystem.maintainPhoneTilt();
        else{
            if(toggleAllowed(gamepad1.b, b)){
                robot.glyphSystem.togglePhoneTilt();
            }
        }


        pwrFwd = pwrDamper * gamepad1.left_stick_y;
        pwrStf = pwrDamper * gamepad1.left_stick_x;
        pwrRot = -pwrDamper * .75 * gamepad1.right_stick_x;

        pwrRot += .33 * (gamepad1.right_trigger - gamepad1.left_trigger);

        pwrFwdL = pwrDamper * gamepad1.left_stick_y;
        pwrStfL = pwrDamper * gamepad1.left_stick_x;

        pwrFwdR = pwrDamper * gamepad1.right_stick_y;
        pwrStfR = pwrDamper * gamepad1.right_stick_x;

        if(enableTank){
            robot.driveMixerMecTank(pwrFwdL, pwrStfL, pwrFwdR, pwrStfR);
        }

        else {
            robot.driveMixerMec(pwrFwd, pwrStf, pwrRot);
        }

//        if(robot.glyphSystem.getMotorLiftPosition() <= 2500) {
//            robot.glyphSystem.setMotorLeft(gamepad2.left_stick_y*beaterDamper);
//            robot.glyphSystem.setMotorRight(-gamepad2.left_stick_y*beaterDamper);
//        }

        if(toggleAllowed(gamepad1.right_bumper, right_bumper)){
            if(pwrDamper != .33){
                pwrDamper = .33;
            }
            else
                pwrDamper = 1.0;
        }

        if(toggleAllowed(gamepad1.a, a)){
            robot.glyphSystem.toggleBelt();
        }


        if(toggleAllowed(gamepad1.x, x)){
            robot.glyphSystem.toggleGrip();
        }

        if(robot.glyphSystem.motorLift.getCurrentPosition() < -50 ) {
            if (toggleAllowed(gamepad1.y, y)) {
                liftVerticalDeposit = false;
                liftDeposit = true;
                liftHome = false;
                liftCollect = false;
            }
        }else if(robot.glyphSystem.motorLift.getCurrentPosition() > 50 ){
            if (toggleAllowed(gamepad1.y, y)) {
                robot.glyphSystem.tiltPhoneUp();
                liftVerticalDeposit = false;
                liftDeposit = false;
                liftHome = false;
                liftCollect = true;
            }
        }

//        if(robot.glyphSystem.motorLift.getCurrentPosition() < 50 && robot.glyphSystem.motorLift.getCurrentPosition() > -50){
//            if(toggleAllowed(gamepad1.dpad_down, dpad_down)){
//                liftVerticalDeposit = false;
//                liftDeposit = false;
//                liftHome = false;
//                liftCollect = true;
//            }
//            else if (toggleAllowed(gamepad1.dpad_up, dpad_up)){
//                liftDeposit = true;
//                liftVerticalDeposit = false;
//                liftHome = false;
//                liftCollect = false;
//            }
//        }


//        if(robot.glyphSystem.motorLift.getCurrentPosition() < 50) {

//            if (gamepad1.dpad_up) {
//                robot.glyphSystem.tiltPhoneUp();
//                robot.glyphSystem.raiseLift2();
//            } else if (gamepad1.dpad_down) {
//                robot.glyphSystem.tiltPhoneUp();
//                robot.glyphSystem.lowerLift2();
//            } else {
//                robot.glyphSystem.stopBelt();
//            }
//        }


        if(toggleAllowed(gamepad1.dpad_up, dpad_up)){
            if(robot.glyphSystem.motorLift.getCurrentPosition() > robot.glyphSystem.liftDeposit - 15){
                liftVerticalDeposit = true;
                liftDeposit = false;
                liftHome = false;
                liftCollect = false;
            }
            else {
                liftVerticalDeposit = false;
                liftDeposit = true;
                liftHome = false;
                liftCollect = false;
            }
        }


        if(toggleAllowed(gamepad1.dpad_down, dpad_down)){
            liftCollect = true;
            liftVerticalDeposit = false;
            liftHome = false;
            liftDeposit = false;
        }
//            if(toggleAllowed(gamepad1.dpad_down, dpad_down)){
//                liftVerticalDeposit = false;
//                liftDeposit = false;
//                liftHome = true;
//                liftCollect = false;
//            }


        if(liftHome){
            if(robot.glyphSystem.goHome()){
                liftHome = false;
            }
        }

        if(liftDeposit){
            if(robot.glyphSystem.goLiftDeposit()){
                liftDeposit = false;
            }
        }

        if(liftVerticalDeposit){
            if(robot.glyphSystem.goLiftVerticalDeposit()){
                liftVerticalDeposit = false;
            }
        }
        if(liftCollect){
            robot.glyphSystem.tiltPhoneUp();
            if(robot.glyphSystem.goLiftCollect()){
                liftCollect = false;
            }
        }
//



//        if(gamepad1.a){
//            robot.glyphSystem.lowerLift2();
//        }
//
//        else if(gamepad1.y){
//            robot.glyphSystem.raiseLift2();
//        }
//
//        else{
//            robot.glyphSystem.stopBelt();
//        }
//
//       if(toggleAllowed(gamepad1.b, b)){
//          robot.glyphSystem.toggleGrip();
//        }
//
//        if(gamepad1.x){ //get phone up out of the way
//
//            robot.glyphSystem.wideOpenGrip();
//            runtime.reset();
//            while (opModeIsActive() && (runtime.seconds() < 1.0)) {
//                telemetry.update();
//            }
//            robot.glyphSystem.tiltPhoneUp();
//        }
//
//        if (gamepad1.dpad_down) robot.glyphSystem.goLiftMin();
//        if (gamepad1.dpad_up) robot.glyphSystem.goLiftMax();
//        if (gamepad1.dpad_left) robot.glyphSystem.goLiftStack();

        if(gamepad1.dpad_left){
            robot.glyphSystem.raiseLift2();
        }
        else if(gamepad1.dpad_right){
            robot.glyphSystem.lowerLift2();
        }
        else{
            robot.glyphSystem.stopBelt();
        }


        if(.4 < robot.glyphSystem.servoBeltLeft.getPosition() && robot.glyphSystem.servoBeltLeft.getPosition() < .6){
            robot.glyphSystem.setMotorLeft(gamepad1.right_stick_y);
            robot.glyphSystem.setMotorRight(-gamepad1.right_stick_y);
        }
        else{
            robot.glyphSystem.collect();
        }
//        if(gamepad1.right_trigger > 0.5)
//            pwrDamper = 1;
//        else
//            pwrDamper = .33;




//        if(robot.glyphSystem.getMotorLiftPosition() < 510) {
//            if (gamepad2.dpad_up) {
//                robot.jewel.liftArm();
//            }
//            if (gamepad2.dpad_down) {
//                robot.jewel.lowerArm();
//            }
//        }
//        else{
//            robot.jewel.liftArm();
//        }






//        if(toggleAllowed(gamepad1.dpad_right, dpad_right)) {
//            damperTimer = futureTime(2f);
//            pwrDamper = .33;
//        }

//        if(damperTimer < System.nanoTime()){
//            pwrDamper = 1;
//        }

        if(toggleAllowed(gamepad2.y, y)){
            enableTank = !enableTank;
        }



//        degreeRot = -gamepad1.right_stick_x * 45; //hard right maps to 45 degree steering

//        if(toggleAllowed(gamepad1.y, y)){
////            robot.setKdDrive(robot.getKdDrive() + 10);
//        }
//
//        if(toggleAllowed(gamepad1.a, a)){
////            robot.setKdDrive(robot.getKdDrive() - 10);
//        }
//
//        if(toggleAllowed(gamepad1.dpad_up, dpad_up)){
//            robot.setKpDrive(robot.getKpDrive() + 0.005);
//        }
//
//        if(toggleAllowed(gamepad1.dpad_down, dpad_down)){
//            robot.setKpDrive(robot.getKpDrive() - 0.005);
//        }
//
//        if (!runDemo && !robot.isBalanceMode())
//            robot.driveMixer(pwrFwd, degreeRot);

    }

    public boolean autoSetup(){
        switch(autoSetupStage) {
            case 0:
                robot.setZeroHeading();
                robot.resetMotors(true);
                robot.glyphSystem.tiltPhoneDown();
                robot.glyphSystem.closeGrip();
                robot.glyphSystem.collect();
                savedVuMarkCodex = getRelicCodex();
                robot.ledSystem.offPos();

                autoSetupStage++;
                break;
            case 1:
                if(robot.jewel.extendArm()){
                    robot.glyphSystem.hold();
                    autoSetupStage++;
                }
                break;
            case 2: //scan vuforia target and deploy jewel arm
//                robot.glyphSystem.goLiftAuto();
//                if (autoTimer < System.nanoTime()) {
//
//
//                    autoTimer = futureTime(1.0f);
                    autoSetupStage++;
//                }
                break;
            case 3: //get the glyph in front after phone is out of the way
//                robot.glyphSystem.goLiftAuto();
//                if (autoTimer < System.nanoTime()) {
//
//                    robot.glyphSystem.collect();
                    autoSetupStage++;
//                }
                break;
            case 4:
//                if (robot.driveForward(false, .05, .15)) {
//                    autoTimer = futureTime(3.0f);
//                    robot.glyphSystem.closeGripTight();
////                    robot.glyphSystem.hold();
//                    robot.glyphSystem.goLiftAuto();
                    robot.resetMotors(true);
                    autoSetupStage++;
//                }
                break;

            case 5:
//                if(autoTimer < System.nanoTime()){
//                    robot.glyphSystem.hold();
//                    robot.resetMotors(true);
                    autoSetupStage++;
//                }
            break;

            case 6:
                if (autoTimer < System.nanoTime()) {
                    robot.resetMotors(true);
                    jewelMatches = robot.doesJewelMatch(isBlue);
                    autoTimer = futureTime(1.5f);

                    if ((isBlue && jewelMatches) || (!isBlue && jewelMatches)) {

                        robot.jewel.hitLeft();
                    } else {

                        robot.jewel.hitRight();
                    }
                    autoSetupStage++;
                }
                break;
            case 7: //small turn to knock off jewel
//                if ((isBlue && jewelMatches)||(!isBlue && jewelMatches)){
//                    if(robot.rotateIMU(13, 2.5)){
//                        autoTimer = futureTime(1.5f);
//                        autoStage++;
//                        robot.resetMotors(true);
//                    }
//                }
//                else{
//                    if(robot.rotateIMU(347, 2.5)){
//                        autoTimer = futureTime(1.5f);
//                        autoStage++;
//                        robot.resetMotors(true);
//                    }
//                }
                if (autoTimer < System.nanoTime()) { //wait for kick
                    robot.jewel.center();

                    autoSetupStage++;
                }
                break;
            case 8:
                if(robot.jewel.retractArm()){
                   if(isBlue) {
                       robot.ledSystem.bluePos();
                       autoSetupStage++;
                   }
                    else
                    {
                        robot.ledSystem.redPos();
                        autoSetupStage++;
                    }

                }
                break;
            case 9:
//                if(robot.getRoll() > 0 && robot.getRoll() < 180){
//                    robot.resetMotors(true);
//                    robot.driveMixerMec(0,0,0);
//                    autoSetupStage++;
//                }
//                if (robot.driveForward(true, .9, .5)) {
//                    robot.resetMotors(true);
                    autoSetupStage++;
//                }
                break;
            case 10:
                if (robot.driveForward(false, .1, .35)) {
                    robot.resetMotors(true);
                     autoSetupStage++;
                }
                break;
            case 11: //lift jewel arm
//                robot.jewel.liftArm();
////                if (autoTimer < System.nanoTime()) {
                    autoSetupStage++;
//                    autoTimer = futureTime(1.5f);
////                }

                break;
            case 12:
//                if(autoTimer < System.nanoTime()){
//                    robot.glyphSystem.collect();
//                    robot.glyphSystem.tiltPhoneUp();
                    autoTimer = futureTime(1.5f);
                    autoSetupStage++;
//                }
                break;
            case 13:
                if(autoTimer < System.nanoTime()){
//                    robot.glyphSystem.closeGripTight();
//
                    autoSetupStage++;
                    robot.resetMotors(true);
                }
                break;
            case 14:
                if(robot.rotateIMU(0, 1)){
                    robot.resetMotors(true);
                    autoSetupStage = 0;
                    return true;
                }
                break;
            case 15:
//                if(robot.driveForward(true, .3, .5)){
//                    robot.resetMotors(true);
//                    robot.glyphSystem.goLiftAuto2();
//                    autoSetupStage = 0;
//                    return true;
//                }
                break;
        }
        return false;
    }

    private void autonomous3() {

        switch(autoStage){
            case 0:
                if(autoSetup()) autoStage = 8;
                break;
            case 8: //turn parallel to the wall
                if(isBlue){
                    if(robot.rotateIMU(270, 3.5)){
                        robot.resetMotors(true);
                        autoStage++;
                    }
                }
                else{
                    if(robot.rotateIMU(90, 3.5)){
                        robot.resetMotors(true);
                        autoStage++;
                    }
                }
                break;
            case 9: //drive off the balance stone
                if(robot.driveForward(false, .6, .5)) {
                    robot.resetMotors(true);
                    autoStage++;
                }
                break;
            case 10: //re-orient robot
                if(isBlue){
                    if(robot.rotateIMU(270, 1.5)){
                        robot.resetMotors(true);
                        autoStage++;
                    }
                }
                else{
                    if(robot.rotateIMU(90, 1.5)){
                        robot.resetMotors(true);
                        autoStage++;
                    }
                }
                break;
            case 11: //turn to proper crypto box column based on vuforia target
                if(isBlue) {
                    switch (savedVuMarkCodex) {
                        case 0:
                            if (robot.driveStrafe(false, .185, .5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                                 //robot.doTheAutonomous("900 points, both cryptoboxes"); robot.plsDoIt();
                            }
                            break;
                        case 1:
                            if (robot.driveStrafe(false, .385, .5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                        case 2:
                            if (robot.driveStrafe(false, .585, .5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                    }
                }
                else{
                    switch (savedVuMarkCodex) {
                        case 0:
                            if (robot.rotateIMU(30, 1.5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                        case 1:
                            if (robot.rotateIMU(40, 1.5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                        case 2:
                            if (robot.rotateIMU(60, 1.5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                    }
                }
                break;
//            case 11: //turn to crypto box
//                if(isBlue){
//                    if(robot.rotateIMU(325, 1.5)){
//                        robot.resetMotors(true);
//                        autoStage++;
//                    }
//                }
//                else{
//                    if(robot.rotateIMU(35, 1.5)){
//                        robot.resetMotors(true);
//                        autoStage++;
//                    }
//                }
//                autoStage++;
//                break;
            case 12: //deposit glyph
                if(robot.driveForward(false, 1.0, .50)) {
                    robot.resetMotors(true);
                    robot.glyphSystem.releaseGrip();
                    robot.glyphSystem.setMotorLeft(-1);
                    robot.glyphSystem.setMotorRight(-1);
                    autoTimer = futureTime(1.5f);
                    autoStage++;
                }
                break;
            case 13:
                if(autoTimer < System.nanoTime()){
                    autoStage++;
                }
                break;
            case 14: //back away from crypto box
                if(robot.driveForward(true, .05, .50)){
                    robot.resetMotors(true);
                    robot.glyphSystem.setMotorLeft(0);
                    robot.glyphSystem.setMotorRight(0);
                    autoStage++;
                }
                break;
            case 15:
                autoTimer = futureTime(1.5f);
                robot.glyphSystem.closeGrip();
                autoStage++;
                break;
            case 16:
                if(autoTimer < System.nanoTime()){
                    autoStage++;
                }
                break;
            case 17:
                //tap glyph to make sure its fully inserted
//                if(robot.driveForward(false, .35, .50)){
//                    robot.resetMotors(true);
                autoStage++;
//                }
                break;
            case 18:
                //back away from crypto box but stay in parking zone
//                if(robot.driveForward(true, .05, .30)){
//                    robot.resetMotors(true);
                autoStage++;
//                }
                break;
            default:
                robot.resetMotors(true);
                autoStage = 0;
                active = false;
                state = 0;
                break;
        }
    }

    public void autonomous(){
        switch(autoStage){
            case 0:
                robot.setZeroHeading();
                robot.resetMotors(true);
                robot.glyphSystem.tiltPhoneDown();
                robot.glyphSystem.closeGrip();
                robot.glyphSystem.collect();
                savedVuMarkCodex = getRelicCodex();
                robot.ledSystem.offPos();
                autoStage++;
                break;
            case 1:
                if(robot.jewel.extendArm()){
                    robot.glyphSystem.hold();
                    autoStage++;
                }
                break;
            case 2: //scan vuforia target and deploy jewel arm
//                robot.glyphSystem.goLiftAuto();
//                if (autoTimer < System.nanoTime()) {
//
//
//                    autoTimer = futureTime(1.0f);
                autoStage++;
//                }
                break;
            case 3: //get the glyph in front after phone is out of the way
//                robot.glyphSystem.goLiftAuto();
//                if (autoTimer < System.nanoTime()) {
//
//                    robot.glyphSystem.collect();
                autoStage++;
//                }
                break;
            case 4:
//                if (robot.driveForward(false, .05, .15)) {
//                    autoTimer = futureTime(3.0f);
//                    robot.glyphSystem.closeGripTight();
////                    robot.glyphSystem.hold();
//                    robot.glyphSystem.goLiftAuto();
                robot.resetMotors(true);
                autoStage++;
//                }
                break;

            case 5:
//                if(autoTimer < System.nanoTime()){
//                    robot.glyphSystem.hold();
//                    robot.resetMotors(true);
                autoStage++;
//                }
                break;

            case 6:
                if (autoTimer < System.nanoTime()) {
                    robot.resetMotors(true);
                    jewelMatches = robot.doesJewelMatch(isBlue);
                    autoTimer = futureTime(1.5f);

                    if ((isBlue && jewelMatches) || (!isBlue && jewelMatches)) {

                        robot.jewel.hitLeft();
                    } else {

                        robot.jewel.hitRight();
                    }
                    autoStage++;
                }
                break;
            case 7: //small turn to knock off jewel
//                if ((isBlue && jewelMatches)||(!isBlue && jewelMatches)){
//                    if(robot.rotateIMU(13, 2.5)){
//                        autoTimer = futureTime(1.5f);
//                        autoStage++;
//                        robot.resetMotors(true);
//                    }
//                }
//                else{
//                    if(robot.rotateIMU(347, 2.5)){
//                        autoTimer = futureTime(1.5f);
//                        autoStage++;
//                        robot.resetMotors(true);
//                    }
//                }
                if (autoTimer < System.nanoTime()) { //wait for kick
                    robot.jewel.center();
                    if(robot.jewel.retractArm()){
                        if(isBlue) {
                            robot.ledSystem.bluePos();
                            autoSetupStage++;
                        }
                        else
                        {
                            robot.ledSystem.redPos();
                            autoSetupStage++;
                        }

                    }
                    autoStage++;
                }
                break;
            case 8:
                if(robot.jewel.retractArm()){
                    autoStage++;
                }
                break;
            case 9:
//                if(robot.getRoll() > 0 && robot.getRoll() < 180){
//                    robot.resetMotors(true);
//                    robot.driveMixerMec(0,0,0);
//                    autoSetupStage++;
//                }
//                if (robot.driveForward(true, .9, .5)) {
//                    robot.resetMotors(true);
                autoStage++;
//                }
                break;
            case 10:
                if (robot.driveStrafe(!isBlue, .5, .5)) {
                    robot.resetMotors(true);
                    autoStage++;
                }
                break;

            case 11: //drive to proper crypto box column based on vuforia target
//
                if (robot.driveStrafe(isBlue, .1, .35)) {
                    robot.resetMotors(true);
                    autoStage++;
                }
                break;
            case 12: //turn to crypto box
                switch (savedVuMarkCodex) {
                    case 0:
                        if(robot.driveStrafe(!isBlue, 0, .35)) {
                            robot.resetMotors(true);
                            autoStage++;
                        }
                        break;
                    case 1:
                        if(robot.driveStrafe(!isBlue, .12, .35)) {
                            robot.resetMotors(true);
                            autoStage++;
                        }
//                        autoStage++;
                        break;
                    case 2:
                        if(robot.driveStrafe(!isBlue, .3, .35)) {
                            robot.resetMotors(true);
                            autoStage++;
                        }
//                        autoStage++;
                        break;
                }
                break;
            case 13: //deposit glyph

                if(isBlue){
                    if(robot.rotateIMU(335, 1.5)){
                        robot.resetMotors(true);
                        autoStage++;
                    }
                }
                else{
                    if(robot.rotateIMU(25, 1.5)){
                        robot.resetMotors(true);
                        autoStage++;
                    }
                }
                break;
            case 14:
                if(robot.glyphSystem.goLiftVerticalDeposit()){
                    autoStage++;
                }
                break;
//            case 11: //turn to crypto box
//                if(isBlue){
//                    if(robot.rotateIMU(325, 1.5)){
//                        robot.resetMotors(true);
//                        autoStage++;
//                    }
//                }
//                else{
//                    if(robot.rotateIMU(35, 1.5)){
//                        robot.resetMotors(true);
//                        autoStage++;
//                    }
//                }
//                autoStage++;
//                break;
            case 15: //deposit glyph
                if(robot.driveForward(true, .5, .50)) {
                    robot.resetMotors(true);
//                    robot.glyphSystem.releaseGrip();
//                    robot.glyphSystem.setMotorLeft(-1);
//                    robot.glyphSystem.setMotorRight(-1);
                    robot.glyphSystem.setMotorLeft(1);
                    robot.glyphSystem.setMotorRight(1);
                    robot.glyphSystem.servoBeltLeft.setPosition(servoNormalize(robot.glyphSystem.beltOn));
                    robot.glyphSystem.servoBeltRight.setPosition(servoNormalize(robot.glyphSystem.beltOn));
                    autoTimer = futureTime(1.5f);
                    autoStage++;
                }
                break;
            case 16:
                if(autoTimer < System.nanoTime()){
                    robot.glyphSystem.releaseGrip();
                    autoTimer = futureTime(1.5f);
                    autoStage++;
                }
                break;
            case 17:
                if(autoTimer < System.nanoTime()){
                    autoStage++;
                }
                break;
            case 18: //back away from crypto box
                    if(robot.driveForward(false, .1, .50)){
                        robot.resetMotors(true);
                        robot.glyphSystem.setMotorLeft(0);
                        robot.glyphSystem.setMotorRight(0);
                        autoStage++;
                    }
            case 19:
                autoTimer = futureTime(1.5f);
                robot.glyphSystem.closeGrip();
                autoStage++;
                break;
            case 20:
                if(autoTimer < System.nanoTime()){
                    autoStage++;
                }
                break;
            case 21:
                //tap glyph to make sure its fully inserted
//                if(robot.driveForward(false, .35, .50)){
//                    robot.resetMotors(true);
                if(robot.glyphSystem.goHome())
                    autoStage++;
//                }
                break;
            case 22:
                //back away from crypto box but stay in parking zone
//                if(robot.driveForward(true, .05, .30)){
//                    robot.resetMotors(true);
                autoStage++;
//                }
                break;
//            case 14:
//                if(autoTimer < System.nanoTime()){
//                    autoStage++;
//                }
//                break;
//            case 15: //back away from crypto box
//                if(robot.driveForward(true, .15, .50)){
//                    robot.resetMotors(true);
//                    autoStage++;
//                }
//                break;
//            case 16:
//                autoTimer = futureTime(1.5f);
//                robot.glyphSystem.closeGrip();
//                autoStage++;
//                break;
//            case 17:
//                if(autoTimer < System.nanoTime()){
//                    autoStage++;
//                }
//                break;
//            case 18:
//                //back away from crypto box
//                if(robot.driveForward(false, .25, .50)){
//                    robot.resetMotors(true);
//                    autoStage++;
//                }
//                break;
//            case 19:
//                //back away from crypto box
//                if(robot.driveForward(true, .05, .30)){
//                    robot.resetMotors(true);
//                    autoStage++;
//                }
//                break;
            default:
                robot.resetMotors(true);
                autoStage = 0;
                active = false;
                state = 0;
                break;
        }
    }
    public void autonomous2 (){

        switch(autoStage){
            case 0:
                if(autoSetup()) autoStage = 8;
                break;
            case 8: //turn parallel to the wall
                if(isBlue){
                    if(robot.rotateIMU(270, 3.5)){
                        robot.resetMotors(true);
                        autoStage++;
                    }
                }
                else{
                    if(robot.rotateIMU(90, 3.5)){
                        robot.resetMotors(true);
                        autoStage++;
                    }
                }
                break;
            case 9: //drive off the balance stone
                if(robot.driveForward(true, .4, .5)) {
                    robot.resetMotors(true);
                    autoStage++;
                }
                break;
            case 10: //re-orient robot
                if(isBlue){
                    if(robot.rotateIMU(270, 1.5)){
                        robot.resetMotors(true);
                        autoStage++;
                    }
                }
                else{
                    if(robot.rotateIMU(90, 1.5)){
                        robot.resetMotors(true);
                        autoStage++;
                    }
                }
                break;
            case 11: //turn to proper crypto box column based on vuforia target
                if(isBlue) {
                    switch (savedVuMarkCodex) {
                        case 0:
                            if (robot.rotateIMU(320, 1.5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                        case 1:
                            if (robot.rotateIMU(310, 1.5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                        case 2:
                            if (robot.rotateIMU(305, 1.5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                    }
                }
                else{
                    switch (savedVuMarkCodex) {
                        case 0:
                            if (robot.rotateIMU(70 , 1.5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                        case 1:
                            if (robot.rotateIMU(50, 1.5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                        case 2:
                            if (robot.rotateIMU(40, 1.5)) {
                                robot.resetMotors(true);
//                                robot.glyphSystem.goLiftAuto();
                                autoStage++;
                            }
                            break;
                    }
                }
                break;
            case 12:
                if(robot.glyphSystem.goLiftVerticalDeposit()){
                    autoStage++;
                }
                break;
//            case 11: //turn to crypto box
//                if(isBlue){
//                    if(robot.rotateIMU(325, 1.5)){
//                        robot.resetMotors(true);
//                        autoStage++;
//                    }
//                }
//                else{
//                    if(robot.rotateIMU(35, 1.5)){
//                        robot.resetMotors(true);
//                        autoStage++;
//                    }
//                }
//                autoStage++;
//                break;
            case 13: //deposit glyph
                if(robot.driveForward(true, .75, .50)) {
                    robot.resetMotors(true);
//                    robot.glyphSystem.releaseGrip();
//                    robot.glyphSystem.setMotorLeft(-1);
//                    robot.glyphSystem.setMotorRight(-1);
                    robot.glyphSystem.setMotorLeft(1);
                    robot.glyphSystem.setMotorRight(1);
                    autoTimer = futureTime(1.5f);
                    autoStage++;
                }
                break;
            case 14:
                if(autoTimer < System.nanoTime()){
                    robot.glyphSystem.releaseGrip();
                    autoTimer = futureTime(1.5f);
                    autoStage++;
                }
                break;
            case 15:
                if(autoTimer < System.nanoTime()){
                    autoStage++;
                }
                break;
            case 16: //back away from crypto box
                if(robot.driveForward(false, .1, .50)){
                    robot.resetMotors(true);
                    robot.glyphSystem.setMotorLeft(0);
                    robot.glyphSystem.setMotorRight(0);
                    autoStage++;
                }
                break;
            case 17:
                autoTimer = futureTime(1.5f);
                robot.glyphSystem.closeGrip();
                autoStage++;
                break;
            case 18:
                if(autoTimer < System.nanoTime()){
                    autoStage++;
                }
                break;
            case 19:
                //tap glyph to make sure its fully inserted
//                if(robot.driveForward(false, .35, .50)){
//                    robot.resetMotors(true);
                if(robot.glyphSystem.goHome())
                    autoStage++;
//                }
                break;
            case 20:
                //back away from crypto box but stay in parking zone
//                if(robot.driveForward(true, .05, .30)){
//                    robot.resetMotors(true);
                    autoStage++;
//                }
                break;
            default:
                robot.resetMotors(true);
                autoStage = 0;
                active = false;
                state = 0;
                break;
        }
    }



    public void resetAuto(){
        autoStage = 0;
        autoTimer = 0;
        robot.resetTPM();
    }


    //the method that controls the main state of the robot; must be called in the main loop outside of the main switch
    public void stateSwitch() {

        /*button indexes:
        0  = a
        1  = b
        2  = x
        3  = y
        4  = dpad_down
        5  = dpad_up
        6  = dpad_left
        7  = dpad_right
        8  = left bumper
        9  = right bumper
        10 = start button
        */

//        if (toggleAllowed(gamepad1.left_bumper, left_bumper)) {
//
//            state--;
//            if (state < 0) {
//                state = 9;
//            }
//            robot.resetMotors(true);
//            active = false;
//            resetAuto();
//            codexFlashStage = 0;
//        }
//
//        if (toggleAllowed(gamepad1.right_bumper, right_bumper)) {
//
//            state++;
//            if (state > 9) {
//                state = 0;
//            }
//            robot.resetMotors(true);
//            active = false;
//            resetAuto();
//            codexFlashStage = 0;
//        }

        if (!active) {
            if(toggleAllowed(gamepad1.b, b)){

                state++;
                if (state > 9) {
                    state = 0;
                }
                robot.resetMotors(true);
                active = false;
                resetAuto();
                    codexFlashStage = 0;

            }
        }

        if (toggleAllowed(gamepad1.start, startBtn)) {
            robot.resetMotors(true);
            active = !active;
            codexFlashStage = 0;
        }
    }


    //checks to see if a specific button should allow a toggle at any given time; needs a rework
    boolean toggleAllowed(boolean button, int buttonIndex)
    {

        /*button indexes:
        0  = a
        1  = b
        2  = x
        3  = y
        4  = dpad_down
        5  = dpad_up
        6  = dpad_left
        7  = dpad_right
        8  = left bumper
        9  = right bumper
        10 = start button
        */

        if (button) {
            if (!buttonSavedStates[buttonIndex])  { //we just pushed the button, and when we last looked at it, it was not pressed
                buttonSavedStates[buttonIndex] = true;
                return true;
            }
            //       else if(buttonCurrentState[buttonIndex] == buttonSavedStates[buttonIndex] && buttonCurrentState[buttonIndex]){
            else { //the button is pressed, but it was last time too - so ignore

                return false;
            }
        }

        buttonSavedStates[buttonIndex] = false; //not pressed, so remember that it is not
        return false; //not pressed

    }


    public String getAlliance(){
        if(isBlue)
            return "Blue";
        return "Red";
    }





    void configureDashboard() {
        // Configure the dashboard.

        // At the beginning of each telemetry update, grab a bunch of data
        // from the IMU that we will then display in separate lines.
        telemetry.addAction(new Runnable() {
            @Override
            public void run() {
                // Acquiring the angles is relatively expensive; we don't want
                // to do that in each of the three items that need that info, as that's
                // three times the necessary expense.
                angles = robot.imu.getAngularOrientation().toAxesReference(AxesReference.INTRINSIC).toAxesOrder(AxesOrder.ZYX);
            }
        });


        telemetry.addLine()
                .addData("active", new Func<String>() {
                    @Override public String value() {
                        return Boolean.toString(active);
                    }
                })
                .addData("state", new Func<String>() {
                    @Override public String value() {
                        return Integer.toString(state);
                    }
                })
                .addData("Servo Tester", new Func<String>() {
                    @Override public String value() {
                        return Integer.toString(robot.servoTesterPos);
                    }
                })

                .addData("lift pos", new Func<String>() {
                    @Override public String value() {
                        return Integer.toString(robot.glyphSystem.getMotorLiftPosition());
                    }
                })
                .addData("servoJewelLeft", new Func<String>() {
                    @Override public String value() {
                        return Integer.toString(robot.jewel.jewelPos);
                    }
                });
//        telemetry.addLine()
//                .addData("Kp", new Func<String>() {
//                    @Override public String value() {
//                        return "" + robot.getKpDrive();
//                    }
//                })
//                .addData("Kd", new Func<String>() {
//                    @Override public String value() {
//                        return "" + robot.getKdDrive();
//                    }
//                });
//
        telemetry.addLine()
//                .addData("phone pos", new Func<String>() {
//                    @Override public String value() {
//                        return Integer.toString(robot.glyphSystem.maintainPhoneTilt());
//                    }
//                })
                .addData("status", new Func<String>() {
                    @Override public String value() {
                        return robot.imu.getSystemStatus().toShortString();
                    }
                })
                .addData("calib", new Func<String>() {
                    @Override public String value() {
                        return robot.imu.getCalibrationStatus().toString();
                    }
                })
//                .addData("Jewel Red", new Func<String>() {
//                    @Override public String value() {
//                        return "" + robot.colorJewel.red();
//                    }
//                })
//
//                .addData("Jewel Blue", new Func<String>() {
//                    @Override public String value() {
//                        return "" + robot.colorJewel.blue();
//                    }
//                })

                .addData("Relic Codex", new Func<String>() {
                    @Override public String value() {
                        return getRelicCodexStr();
                    }
                })
                .addData("Relic Codex", new Func<String>() {
                    @Override public String value() {
                        return Integer.toString(savedVuMarkCodex);
                    }
                });

        telemetry.addLine()
                .addData("heading", new Func<String>() {
                    @Override public String value() {
                        //return formatAngle(angles.angleUnit, angles.firstAngle);
                        return Double.toString(robot.getHeading());
                    }
                })
                .addData("pitch", new Func<String>() {
                    @Override public String value() {
                        //return formatAngle(angles.angleUnit, angles.firstAngle);
                        return Double.toString(robot.getPitch());
                    }
                })
                .addData("roll", new Func<String>() {
                    @Override public String value() {
                        //return formatAngle(angles.angleUnit, angles.firstAngle);
                        return Double.toString(robot.getRoll());
                    }
                })
                .addData("glyph roll", new Func<String>() {
                    @Override public String value() {
                        //return formatAngle(angles.angleUnit, angles.firstAngle);
                        return Double.toString(robot.glyphSystem.roll);
                    }
                })
                .addData("glyph ticks", new Func<String>() {
                    @Override public String value() {
                        //return formatAngle(angles.angleUnit, angles.firstAngle);
                        return Integer.toString(robot.glyphSystem.getMotorLiftPosition());
                    }
                });
//                .addData("headingRaw", new Func<String>() {
//                    @Override public String value() {
//                        return formatAngle(angles.angleUnit, angles.firstAngle);
//
//                    }
//                })
//                .addData("headingOffset", new Func<String>() {
//                    @Override public String value() {
//                        return Double.toString(robot.offsetHeading);
//
//                    }
//                })
//
//                .addData("rollRaw", new Func<String>() {
//                    @Override public String value() {
//                        //return formatAngle(angles.angleUnit, angles.secondAngle);
//                        return Double.toString(robot.getRoll());
//                    }
//                })
//                .addData("pitchRaw", new Func<String>() {
//                    @Override public String value() {
//                        return formatAngle(angles.angleUnit, angles.thirdAngle);
//                    }
//                });
        telemetry.addLine()
                .addData("auto stage", new Func<String>() {
                    @Override public String value() {
                        return String.valueOf(autoStage);
                    }
                })
                //.addData("glyph distance", new Func<String>() {
                //    @Override public String value() {
                //        return String.valueOf(robot.glyphUpper.getDistance(DistanceUnit.CM));
                //    }
                //})
//                .addData("TicksFL", new Func<String>() {
//                    @Override public String value() {
//                        return Long.toString(robot.motorFront.getCurrentPosition());
//                    }
//                })
//                .addData("TicksBL", new Func<String>() {
//                    @Override public String value() {
//                        return Long.toString(robot.motorBack.getCurrentPosition());
//                    }
//                })
                .addData("TicksAvg", new Func<String>() {
                    @Override public String value() {
                        return Long.toString(robot.getAverageTicks());
                    }
                });
        telemetry.addLine()

                .addData("PID Calc", new Func<String>() {
                    @Override public String value() {
                        return Double.toString(robot.drivePID.performPID() );
                    }
                })
                .addData("PID Err", new Func<String>() {
                    @Override public String value() {
                        return Double.toString(robot.drivePID.getError());
                    }
                });
    }
    String formatAngle(AngleUnit angleUnit, double angle) {
        return formatDegrees(AngleUnit.DEGREES.fromUnit(angleUnit, angle));
    }

    String formatDegrees(double degrees){
        return String.format(Locale.getDefault(), "%.1f", AngleUnit.DEGREES.normalize(degrees));
    }

    long futureTime(float seconds){
        return System.nanoTime() + (long) (seconds * 1e9);
    }
}
