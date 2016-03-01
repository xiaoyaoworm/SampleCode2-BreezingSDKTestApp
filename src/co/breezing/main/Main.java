package co.breezing.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import co.breezing.metabolism.parameter.Parameter;
import co.breezing.module.R;
import co.breezing.module.one.qrcode.QRcodeFromFile;
import co.breezing.module.one.qrcode.QRcodeParse;
import co.breezing.module.one.qrcode.SaveQRFile;
import co.breezing.module.one.qrcode.thirdlib.capture.CaptureActivity;
import co.breezing.module.six.bluetooth.Bluetooth;
import co.breezing.module.six.bluetooth.MACFromFile;

public class Main extends Activity {

	private static String tag = "Main";

	private CheckBox checkInfo_mouthpiece;
	private CheckBox checkInfo_openDevice;
	private CheckBox checkInfo_insert;
	private CheckBox checkInfo_qrcode;
	private Button connect;
	private TextView checkTitle;
	public static BluetoothAdapter btAdap = null;

	private QRcodeParse qrcodeParse;
	
	public int connectionTime = 0;
	public static ProgressDialog pd;
	private boolean start_flag = false;
	private boolean finish_flag = false;
	private Bluetooth bluetooth = new Bluetooth();
	private boolean connectionEstablished = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btAdap = Bluetooth.getInstance().getBluetoothAdapter();
		
		checkInfo_mouthpiece = (CheckBox) findViewById(R.id.checkInfo_mouthpiece);
		checkInfo_openDevice = (CheckBox) findViewById(R.id.checkInfo_openDevice);
		checkInfo_insert = (CheckBox) findViewById(R.id.checkInfo_insert);
		checkInfo_qrcode = (CheckBox) findViewById(R.id.checkInfo_qrcode);
		connect = (Button) findViewById(R.id.connect);
		checkTitle = (TextView) findViewById(R.id.checkTitle);

		qrcodeParse = new QRcodeParse();

		// training part
		if (Parameter.training_flag == true) {
			checkInfo_mouthpiece.setChecked(false);
			checkInfo_openDevice.setChecked(false);
			checkInfo_insert.setChecked(false);
			checkInfo_openDevice.setVisibility(View.INVISIBLE);
			checkInfo_insert.setVisibility(View.INVISIBLE);
			connect.setVisibility(View.INVISIBLE);
			checkInfo_qrcode.setVisibility(View.GONE);
			new AlertDialog.Builder(Main.this)
					.setTitle("欢迎")
					.setMessage("张三，您好。\n您即将开始您的训练代谢测试。")
					.setNeutralButton("确认",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
								}
							})

					.show();
		}
		else {
			checkInfo_qrcode.setVisibility(View.GONE);
			checkInfo_mouthpiece.setVisibility(View.INVISIBLE);
			checkInfo_openDevice.setVisibility(View.INVISIBLE);
			checkInfo_insert.setVisibility(View.INVISIBLE);
			connect.setVisibility(View.INVISIBLE);
			checkTitle.setVisibility(View.INVISIBLE);

			new AlertDialog.Builder(Main.this)
					.setTitle("Breezing")
					.setMessage("您是否扫描过QR码？")
					.setCancelable(false)
					.setNeutralButton("否",
							new DialogInterface.OnClickListener() {
								public void onClick(
										DialogInterface dialoginterface, int i) {

									try {
										new AlertDialog.Builder(Main.this)
												.setTitle("扫描QR码")
												.setMessage("是否现在扫描QR码")
												.setCancelable(false)
												.setNegativeButton(
														"取消",
														new DialogInterface.OnClickListener() {

															public void onClick(
																	DialogInterface dialog,
																	int which) {
																finish();
															}
														})
												.setNeutralButton(
														"现在扫描",
														new DialogInterface.OnClickListener() {
															public void onClick(
																	DialogInterface dialog,
																	int whichButton) {
																Intent myIntent = new Intent(
																		getApplicationContext(),
																		CaptureActivity.class);
																myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
																startActivityForResult(
																		myIntent,
																		Parameter.qrscan_requestcode);
															}
														}).show();

									}
									catch (Exception e) {
										Log.d("main activity: ",
												"Exception in capturing image: "
														+ e.toString());
									}

								}
							})
					.setNegativeButton("是",
							new DialogInterface.OnClickListener() {
								public void onClick(
										DialogInterface dialoginterface, int i) {
									String qrcode = QRcodeFromFile
											.getQrcodeFromFile();
									QRcodeParse result = qrcodeParse
											.read_qr_data(qrcode);
									if (result == null) {
										Toast.makeText(getBaseContext(),
												"已存的QR码解析错误，请重新扫描",
												Toast.LENGTH_LONG).show();
										finish();
									}
									else {
										Parameter.qrcode = qrcode;
										Parameter.MAC = MACFromFile.getMACFromFile();
										connectionTime = 0;
										connectWithDevice(Parameter.MAC);
									}
								}
							}).show();

			// checkInfo_insert.setText("插入正常芯片");

			Intent intent = getIntent();
			if (intent.hasExtra("qrcode")) {
				String qrcode = intent.getStringExtra("qrcode");
				if (qrcode == null) {
					Toast.makeText(getBaseContext(), "没有找到QR码，请重新扫描",
							Toast.LENGTH_LONG).show();
					finish();
				}
				else {
					QRcodeParse result = qrcodeParse.read_qr_data(qrcode);
					if (result == null) {
						Toast.makeText(getBaseContext(), "QR码有误，请扫描正确的QR码",
								Toast.LENGTH_LONG).show();
						finish();
					}
					else {
						SaveQRFile.saveQRcode(getBaseContext(), qrcode);
						Parameter.qrcode = qrcode;
						Parameter.MAC = MACFromFile.getMACFromFile();
						connectionTime = 0;
						connectWithDevice(Parameter.MAC);
					}
				}
			}
			else {
				checkInfo_qrcode.setVisibility(View.GONE);
				checkInfo_mouthpiece.setVisibility(View.INVISIBLE);
			}
		}

		connect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
					Intent myIntent = new Intent(getBaseContext(),
							DeviceList.class);
					startActivityForResult(myIntent, 0);
			}
		});

		checkInfo_qrcode
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked == true) {
							new AlertDialog.Builder(Main.this)
									.setTitle("Breezing")
									.setMessage("您是否扫描过QR码？")
									.setCancelable(false)
									.setNeutralButton(
											"否",
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialoginterface,
														int i) {

													try {
														new AlertDialog.Builder(
																Main.this)
																.setTitle(
																		"扫描QR码")
																.setMessage(
																		"是否现在扫描QR码")
																.setCancelable(
																		false)
																.setNegativeButton(
																		"取消",
																		new DialogInterface.OnClickListener() {

																			public void onClick(
																					DialogInterface dialog,
																					int which) {
																				checkInfo_qrcode
																						.setChecked(false);
																			}
																		})
																.setNeutralButton(
																		"现在扫描",
																		new DialogInterface.OnClickListener() {
																			public void onClick(
																					DialogInterface dialog,
																					int whichButton) {
																				Intent myIntent = new Intent(
																						getApplicationContext(),
																						CaptureActivity.class);
																				myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
																				startActivityForResult(
																						myIntent,
																						Parameter.qrscan_requestcode);
																			}
																		})
																.show();

													}
													catch (Exception e) {
														Log.d("main activity: ",
																"Exception in capturing image: "
																		+ e.toString());
													}

												}
											})
									.setNegativeButton(
											"是",
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialoginterface,
														int i) {
													String qrcode = QRcodeFromFile
															.getQrcodeFromFile();
													if (qrcode == null) {
														checkInfo_qrcode
																.setChecked(false);
														Toast.makeText(
																getBaseContext(),
																"没有找到QR码，请重新扫描",
																Toast.LENGTH_LONG)
																.show();
													}
													else {
														QRcodeParse result = qrcodeParse
																.read_qr_data(qrcode);
														if (result == null) {
															Toast.makeText(
																	getBaseContext(),
																	"已存的QR码解析错误，请重新扫描",
																	Toast.LENGTH_LONG)
																	.show();
														}
														else {
															Parameter.qrcode = qrcode;
															checkInfo_qrcode
																	.setEnabled(false);
															checkInfo_mouthpiece
																	.setVisibility(View.VISIBLE);
														}
													}
												}
											}).show();
						}
					}

				});
		checkInfo_mouthpiece
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						checkInfo_openDevice.setVisibility(View.VISIBLE);
						checkInfo_mouthpiece.setEnabled(false);
					}
				});

		checkInfo_openDevice
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						checkInfo_insert.setVisibility(View.VISIBLE);
						checkInfo_openDevice.setEnabled(false);
					}
				});

		checkInfo_insert
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						connect.setVisibility(View.VISIBLE);
						checkInfo_insert.setEnabled(false);
					}
				});
	}

	
	/**
	 * functino to connect with device and show the progress dialog
	 */
	public void connectWithDevice(String address) {
		finish_flag = false;
		start_flag = false;
		BTConnectionTask btTask = new BTConnectionTask();
		btTask.execute();
		pd = new ProgressDialog(Main.this);
		pd.setCanceledOnTouchOutside(false);
		pd.setMessage("正在连接设备" + " (" + address + ")");
		pd.setIndeterminate(true);
		pd.setCancelable(true);
		pd.setButton(DialogInterface.BUTTON_NEGATIVE, "停止连接",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						disconnectWithDevice();
						pd.dismiss();
						pd.cancel();
						finish();
					}
				});
		if (!pd.isShowing())
			pd.show();
	}

	/**
	 * function to disconnect with device
	 */
	public void disconnectWithDevice() {
		connectionTime = 3;
		bluetooth.disconnectBTSocket();
	}

	public class BTConnectionTask extends AsyncTask<Void, Void, Boolean> {

		protected Boolean doInBackground(Void... x) {
			connectionEstablished = false;
			connectionEstablished = bluetooth.connectBTSocket(Parameter.MAC);
			return connectionEstablished;
		}

		protected void onPostExecute(Boolean result) {
			Log.d(tag, "Value of connectionetablished on postExecute: "
					+ connectionEstablished);
			if (connectionEstablished == true) {
				// initial results_flag as 0
				// metabolismAlgo.results_flag = 0;
				if (finish_flag != true) {
					Intent myIntent = new Intent(getBaseContext(),
							Metabolism.class);
					startActivityForResult(myIntent, 0);
					pd.dismiss();
					pd.cancel();
					finish();
				}
			}
			else {
				connectionTime++;
				if (connectionTime < 3) {
					pd.dismiss();
					pd.cancel();
					connectWithDevice(Parameter.MAC);
				}
				else {
					disconnectWithDevice();
					pd.dismiss();
					pd.cancel();
					Toast.makeText(getBaseContext(),
							"连接Breezing设备失败。请检查是否选择正确或是否已打开Breezing设备。",
							Toast.LENGTH_LONG).show();
					finish();
				}
			}
		}
	}
}
