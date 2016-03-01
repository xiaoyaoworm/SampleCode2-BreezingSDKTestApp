package co.breezing.main;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import co.breezing.math.round.RoundData;
import co.breezing.metabolism.parameter.Parameter;
import co.breezing.module.R;
import co.breezing.module.eight.flowcalc.FlowCalculation;
import co.breezing.module.eleven.reerq.REERQCalc;
import co.breezing.module.five.adaptionReference.AdaptionReference;
import co.breezing.module.four.guidance.Biofeedback;
import co.breezing.module.four.guidance.Guidance;
import co.breezing.module.nine.absorbcalc.AbsorbanceCalc;
import co.breezing.module.one.qrcode.QRcodeParse;
import co.breezing.module.seven.dataTranslation.DataTranslation;
import co.breezing.module.seven.dataTranslation.RawData;
import co.breezing.module.six.bluetooth.Bluetooth;
import co.breezing.module.ten.concentration.ConcentrationCalc;
import co.breezing.module.three.adaptionSuggestion.AdaptionSuggestion;
import co.breezing.module.two.pdcheck.PDCheck;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

public class Metabolism extends Activity {

	private TextView suggestion_str;
	private Button stopButton;

	public final static int PLOT_SIZE = 60;
	public XYPlot breathSignalPlot = null;
	private static double breathSignalPlot_minValue = 1.2;
	private static double breathSignalPlot_maxValue = 2.4;
	public SimpleXYSeries pressureSeries = new SimpleXYSeries("Pressure");
	public LinkedList<Number> pressureData = new LinkedList<Number>();
	private static int tl = 0;

	// bluetooth connection status
	public boolean connectionEstablished = false;
	public int connectionTime = 0;
	private static int pd_check_result;

	// tag
	private static String tag = "Metabolism";

	public static Timer timer;
	private static int index = 0;

	public static String qrcode;
	public static int flag;

	private int cycle_number = 0;

	public static Biofeedback biofeedback;

	public static ProgressDialog pd;

	private int errorData = 0;
	private boolean start_flag = false;
	private boolean finish_flag = false;

	// // inital_flag can be considered as the flag showed we get non-zero data,
	// // this is used to start check errData
	private boolean init_flag = false;
	private int data_nonzero_time = 0;

	private static ImageView img;
	private static AnimationDrawable animation;
	private static TextView title;

	private Bluetooth bluetooth = new Bluetooth();
	private DataTranslation data = new DataTranslation();
	private QRcodeParse qrcodeParse = new QRcodeParse();
	private RawData rawData = new RawData();
	private FlowCalculation flowCalculation = new FlowCalculation();
	private AdaptionReference adaptionRef = new AdaptionReference();
	private AdaptionSuggestion adaptionAlgo = new AdaptionSuggestion();
	private AbsorbanceCalc absorbanceCalc = new AbsorbanceCalc();
	private ConcentrationCalc concentrationCalc = new ConcentrationCalc();
	private QRcodeParse qrData = new QRcodeParse();
	private REERQCalc reeRqCal = new REERQCalc();
	private Guidance guidance = new Guidance();
	private AbsorbanceCalc delta_data = new AbsorbanceCalc();

	private FlowCalculation adaptionData = new FlowCalculation();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_metabolism);

		suggestion_str = (TextView) findViewById(R.id.suggestion);
		title = (TextView) findViewById(R.id.title);
		stopButton = (Button) findViewById(R.id.stopButton);

		breathSignalPlot = (XYPlot) findViewById(R.id.breathingsignal);
		breathSignalPlot.setRangeBoundaries(breathSignalPlot_minValue,
				breathSignalPlot_maxValue, BoundaryMode.FIXED);
		breathSignalPlot.setDomainBoundaries(0, 60, BoundaryMode.FIXED);

		breathSignalPlot.addSeries(
				pressureSeries,
				LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.rgb(72, 118, 255), Color.rgb(
						24, 116, 205), Color.TRANSPARENT));
		breathSignalPlot.setDomainStepValue(5);
		breathSignalPlot.setTicksPerRangeLabel(3);
		breathSignalPlot.setDomainLabel("--> Time (x0.25s)");
		breathSignalPlot.getDomainLabelWidget().pack();
		breathSignalPlot.setDomainValueFormat(new DecimalFormat("0"));
		breathSignalPlot.setRangeLabel("");
		breathSignalPlot.getRangeLabelWidget().pack();
		breathSignalPlot.getGraphWidget().setPaddingLeft(-34);
		breathSignalPlot.getLayoutManager().remove(
				breathSignalPlot.getLegendWidget());
		breathSignalPlot.getGraphWidget().getGridBackgroundPaint()
				.setColor(Color.rgb(250, 250, 250));
		breathSignalPlot.getGraphWidget().getGridLinePaint()
				.setColor(Color.rgb(200, 200, 200));
		breathSignalPlot.getGraphWidget().getGridLinePaint().setAlpha(100);
		breathSignalPlot.getGraphWidget().getDomainOriginLinePaint()
				.setAlpha(0);
		breathSignalPlot.getGraphWidget().getRangeOriginLinePaint().setAlpha(0);
		breathSignalPlot.disableAllMarkup();

		title.setText("Breezing初始化中...");

		stopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (start_flag == true) {
					stopTesting();
				}
				else {
					finish_flag = true;
					disconnectWithDevice();
					Intent myIntent = new Intent(getBaseContext(), FirstActivity.class);
					myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(myIntent);
					finish();
				}
			}
		});

		initialAllData();
		connectionEstablished = bluetooth.sendCommand(Bluetooth.getInstance());
		if (connectionEstablished == true) {
			createTimerTask();
		}
		else {
			Log.d(tag, "connection is not established.");
		}

		pd = new ProgressDialog(this);
		pd.setCanceledOnTouchOutside(false);
		pd.setMessage("Breezing初始化中，此时请勿晃动Breezing设备，请稍候...");
		pd.setIndeterminate(true);
		pd.setCancelable(true);
		if (!pd.isShowing())
			pd.show();
	}

	/**
	 * functino to connect with device and show the progress dialog
	 */
	public void connectWithDevice() {
		finish_flag = false;
		start_flag = false;
		BTConnectionTask btTask = new BTConnectionTask();
		btTask.execute();
	}

	/**
	 * function to disconnect with device
	 */
	public void disconnectWithDevice() {
		try {
			if (timer != null) {
				timer.cancel();
				biofeedback.stopSound();
				animation.stop();
			}
			bluetooth.disconnectBTSocket();
		}
		catch (Exception e) {
		}
	}

	public class BTConnectionTask extends AsyncTask<Void, Void, Boolean> {

		protected Boolean doInBackground(Void... x) {
			connectionEstablished = false;
			connectionEstablished = bluetooth.connectBTSocket(Parameter.MAC);
			connectionEstablished = bluetooth.sendCommand(Bluetooth
					.getInstance());
			return connectionEstablished;
		}

		protected void onPostExecute(Boolean result) {
			Log.d(tag, "Value of connectionetablished on postExecute: "
					+ connectionEstablished);
			if (connectionEstablished == true) {
				// initial results_flag as 0
				// metabolismAlgo.results_flag = 0;
				createTimerTask();
			}
			else {
				connectionTime++;
				if (connectionTime < 3) {
					connectWithDevice();
				}
				else {
					Log.d(tag, "failed three time in bluetooth connection");
				}
			}
		}
	}

	public void createTimerTask() {

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			public Handler updateUI = new Handler() {
				@Override
				public void dispatchMessage(Message msg) {
					super.dispatchMessage(msg);
					// Module7: bytes translation
					boolean thirtyFourBytes_flag = rawData.readBytes();
					if (thirtyFourBytes_flag == true) {
						byte[] bytes = RawData.getInstance().getBufferData();
						boolean data_non_zero = data.dataExists(bytes);
						if (data_non_zero == false) {
							data_nonzero_time++;
						}
						else if (data_non_zero == true && data_nonzero_time >= 5
								&& init_flag == false) {
							init_flag = true;
						}
						boolean data_correct = data.calcUsefulData(bytes);
						if (data_correct == true) {
							index++;
							if (data.getVolume() > 5000) {
								start_flag = true;
							}
							if (index == 2) {
								// training part check
								// Module 2: pd check
								pd_check_result = PDCheck.check_cartridge(data,
										Parameter.training_flag);
								String mes = null;
								switch (pd_check_result) {
								case (0): {
									mes = "请插入正式芯片并重启设备。";
									title.setText("芯片有误");
									break;
								}
								case (1): {
									mes = "请插入正式芯片并重启设备。";
									title.setText("芯片有误");
									break;
								}
								case (2): {
									mes = "请重新插入芯片，保证黄色区域位于芯片的右下角，并重启设备.";
									title.setText("芯片有误");
									break;
								}
								case (3): {
									mes = "芯片正常。";
									break;
								}
								case (4): {
									mes = "请插入您的测试芯片并重启设备。";
									title.setText("芯片有误");
									break;
								}
								}
								suggestion_str.setText(mes);
								pd.dismiss();
								pd.cancel();
								/************ for testing ***********/
								// set 3 as default, for testing
								// pd_check_result = 3;
								/************ for testing ***********/
							}
							else if (index > 2) {
								/************ for testing ***********/
								// set 3 as default, for testing
								// pd_check_result = 3;
								/************ for testing ***********/
								if (pd_check_result == PDCheck.GOOD_CARTRIDGE) {
									if (index == 3) {
										// Module 5: adaption reference
										// ref will be stored in
										// adaptionRef.getInstance()
										adaptionRef = adaptionRef
												.adaptionRefAlgo(
														Parameter.weight,
														Parameter.height,
														Parameter.gender,
														Parameter.age);

										pd.dismiss();
										pd.cancel();
										title.setText("放松并开始吹气");

										// play music and play animation
										img = (ImageView) findViewById(R.id.musicAnim);
										biofeedback = new Biofeedback();
										biofeedback
												.initSounds(getBaseContext());
										biofeedback.addSound(1,
												R.raw.testmusic1);
										// createTimerTask();
										guidance.startMusicalGuidance(biofeedback);
										// start animation
										animation = new AnimationDrawable();
										guidance.startAnimation(animation,
												getResources());

//										img.setBackground(animation);
										img.post(new Starter());
									}

									// Module 8: flow calculation
									boolean adaption_cycle_flag = flowCalculation
											.checkCycle(data, index - 2);
									if (adaption_cycle_flag == true) {
										cycle_number++;

										if (cycle_number >= 2) {
											// adaption data will be stored
											// in
											// adaptionData
											adaptionData = flowCalculation
													.calcRealSuggData();
											// Module 3
											// flowCalculation will equal==
											// to
											// null when it is the second
											// cycle,
											// which will be abandoned in
											// adaption suggestion part
											String suggestion = adaptionAlgo
													.adaptionAlgorithm(
															adaptionData,
															adaptionRef);
											suggestion_str.setText(suggestion);
											title.setText(RoundData
													.roundOneDecimals(adaptionData
															.getFreq_sugg())
													+ "次/分");
										}
									}

									// Module 9
									// check the cycle, when it is one
									// cycle, calculate the cycle's
									// delta data
									delta_data = absorbanceCalc.calcAbsor(data,
											index - 2, delta_data);

									// draw plots
									drawPlots(data);

									// Check Whether the testing is finished
									if (data.getStatus() == 1) {
										// mark test is finished
										String currentDateTimeString = DateFormat
												.getDateTimeInstance().format(
														new Date());
										Log.d(tag, "finish time = "
												+ currentDateTimeString);
										Parameter.test_finish_time = System
												.currentTimeMillis();
										finish_flag = true;
										disconnectWithDevice();
										Parameter.time++;

										// Module 8
										// calculate final breath frequency
										// and save it into breathfreq.csv
										// also calculate total ve, which
										// will be used in module 11
										FlowCalculation bf_Ve_Data = flowCalculation
												.flowCalcuProcess(
														data,
														delta_data,
														index - 2,
														getApplicationContext(),
														Parameter.time);
										if (Parameter.training_flag == false) {
											// Module 9
											// calculate whole total_delta
											// data,
											// which will be used in module
											// 10
											AbsorbanceCalc total_delta_data = absorbanceCalc
													.calcDeltaData(
															data,
															delta_data,
															getApplicationContext());
											// Module 10
											// calculate con_co2 and con_o2
											// based on the whole
											// total_delta
											// data
											qrData = qrcodeParse
													.read_qr_data(Parameter.qrcode);
											ConcentrationCalc concenData = concentrationCalc
													.calcConcenData(qrData,
															total_delta_data);
											// Module 11
											// calculate ree and rq based on
											// ve (Module8) and concenData
											// (Module10)
											REERQCalc ree_rq_data = reeRqCal
													.calculateREE_RQ(
															concenData,
															bf_Ve_Data,
															getApplicationContext());
											Log.d(tag,
													"REE = "
															+ ree_rq_data
																	.getRee());
											Log.d(tag,
													"RQ = "
															+ ree_rq_data
																	.getRq());
											Intent myIntent = new Intent(
													getBaseContext(),
													Results.class);
											myIntent.putExtra("type", "normal");
											myIntent.putExtra("rq",
													ree_rq_data.getRq());
											myIntent.putExtra("ree",
													ree_rq_data.getRee());
											startActivityForResult(myIntent, 0);
											finish();
											// results_str.setText("REE = "
											// + ree_rq_data.getRee()
											// + "\n" + "RQ = "
											// + ree_rq_data.getRq());
										}
										else {
											String training_conclusion = adaptionAlgo
													.adaptConcluAlgorithm(
															adaptionData,
															adaptionRef,
															getBaseContext(),
															bf_Ve_Data);
											Intent myIntent = new Intent(
													getBaseContext(),
													Results.class);
											myIntent.putExtra("type",
													"training");
											myIntent.putExtra("conclusion",
													training_conclusion);
											startActivityForResult(myIntent, 0);
											finish();
											// results_str
											// .setText(training_conclusion);
											// suggestion_str.setText("");
										}
									}
								}
								else {
									finish_flag = true;
									disconnectWithDevice();
									animation = new AnimationDrawable();
								}
							}
						}
						else {
							errorData++;
							if (init_flag == true && errorData > 4) {
								bluetooth.quickDisconnect(timer,
										bluetooth.getBtSocket());
								Log.d(tag,
										"Connect function on after quick disconnect");
								connectWithDevice();
								errorData = 0;
							}
							Log.d(tag, "raw data is not correct.");
						}
						Log.d(tag, "readbytes");
					}
					else {
						errorData++;
						if (start_flag == true && errorData > 4) {
							bluetooth.quickDisconnect(timer,
									bluetooth.getBtSocket());
							Log.d(tag,
									"Connect function on after quick disconnect");
							connectWithDevice();
							errorData = 0;
						}
						Log.d(tag, "raw data is not 34 bytes");
					}
				}
			};

			public void run() {
				try {
					if (finish_flag == true) {
						timer.cancel();
						timer.purge();
						return;
					}
					updateUI.sendEmptyMessage(0);
				}
				catch (Exception e) {
					Log.d(tag, "caught Exception in run() Handler" + e);
				}
			}

		}, 0, 250); // every 0.25s
	}

	/** To start the feather animation */
	public class Starter implements Runnable {
		public void run() {
			animation.start();
		}
	}

	private void drawPlots(DataTranslation data) {
		try {
			Number[] dataP = { (data.getPressure() * 0.000001) };
			pressureSeries.setModel(Arrays.asList(dataP),
					SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

			if (pressureData.size() > PLOT_SIZE) {
				pressureData.removeFirst();
			}

			// add the latest history sample:

			pressureData.addLast(data.getPressure() * 0.000001);
			if (tl == 0) {
				pressureData.removeLast();
			}
			pressureSeries.setModel(pressureData,
					SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
			Log.d(tag, "Dynamic plot: " + data.getPressure() + " "
					+ breathSignalPlot_maxValue);
			if ((data.getPressure() * 0.000001) > breathSignalPlot_maxValue) {
				breathSignalPlot_maxValue = (data.getPressure() * 0.000001) + 0.2;
			}
			breathSignalPlot.setRangeBoundaries(1.2,
					breathSignalPlot_maxValue + 0.2, BoundaryMode.FIXED);

			tl++;

			// draw the Pressure plot
			breathSignalPlot.redraw();

		}
		catch (Exception e) {
			Log.d(tag, "Exceptin in plottinf pressure/breathing signal " + e);
		}
	}

	private void initialAllData() {
		// initial all the data will be used
		errorData = 0;
		index = 0;

		flag = 0;
		cycle_number = 0;
		tl = 0;

		timer = new Timer();
		biofeedback = new Biofeedback();
		start_flag = false;
		finish_flag = false;

		bluetooth = new Bluetooth();
		data = new DataTranslation();
		qrcodeParse = new QRcodeParse();
		rawData = new RawData();
		flowCalculation = new FlowCalculation();
		adaptionRef = new AdaptionReference();
		adaptionAlgo = new AdaptionSuggestion();
		adaptionData = new FlowCalculation();
		absorbanceCalc = new AbsorbanceCalc();
		concentrationCalc = new ConcentrationCalc();
		qrData = new QRcodeParse();
		reeRqCal = new REERQCalc();
		guidance = new Guidance();
		delta_data = new AbsorbanceCalc();
	}

	/** Warn the User if test is going on. Else, go back to previous screen */
	public void onBackPressed() {
		if (start_flag == true) {
			stopTesting();
		}
		else {
			super.onBackPressed();
		}
	}

	public void stopTesting() {
		new AlertDialog.Builder(this)
				.setTitle("Breezing")
				.setMessage("是否中止测试?")
				.setNeutralButton("否", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialoginterface, int i) {
					}
				})
				.setPositiveButton("是", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialoginterface, int i) {
						try {
							finish_flag = true;
							Parameter.test_finish_time = System
									.currentTimeMillis();
							disconnectWithDevice();
							Intent myIntent = new Intent(getBaseContext(),
									FirstActivity.class);
							myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(myIntent);
							finish();
						}
						catch (Exception e) {
							Log.d(tag, "Exception in stop all " + e);
						}
					}
				}).show();
	}
}
