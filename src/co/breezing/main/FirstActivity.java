package co.breezing.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import co.breezing.metabolism.parameter.Parameter;
import co.breezing.module.R;
import co.breezing.module.four.guidance.BfAndTimeFromFile;

public class FirstActivity extends Activity implements
		CompoundButton.OnCheckedChangeListener {

	private Button button;

//	private Switch switchButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_first);
		Parameter.time = Integer.valueOf(BfAndTimeFromFile
				.getBfAndTimeFromFile()[4]);
		button = (Button) findViewById(R.id.startButton);

//		switchButton = (Switch) findViewById(R.id.switchButton);

		if (Parameter.time <= 3) {
//			switchButton.setVisibility(View.INVISIBLE);
			Parameter.training_flag = true;
		}
		else {
//			switchButton.setChecked(false);
			Parameter.training_flag = false;
		}
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int wait_time = (int) Math.ceil((300000 - (System
						.currentTimeMillis() - Parameter.test_finish_time)) / 60000);
//				if (System.currentTimeMillis() >= Parameter.test_finish_time + 300000) {
					Intent myIntent = new Intent(getBaseContext(), Main.class);
					startActivityForResult(myIntent, 0);
//				}
//				else {
//					Toast.makeText(getBaseContext(),
//							"设备重置中，请耐心等待" + (wait_time + 1) + "分钟",
//							Toast.LENGTH_LONG).show();
//				}
			}
		});

//		if (switchButton != null) {
//			switchButton.setOnCheckedChangeListener(this);
//		}

	}


	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		Toast.makeText(this, "Training mode is " + (isChecked ? "on" : "off"),
				Toast.LENGTH_SHORT).show();
		if (isChecked) {
			// do stuff when Switch is ON
			Parameter.training_flag = true;
		}
		else {
			// do stuff when Switch if OFF
			Parameter.training_flag = false;
		}
	}

}
