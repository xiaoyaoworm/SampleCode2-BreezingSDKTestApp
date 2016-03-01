package co.breezing.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import co.breezing.metabolism.parameter.Parameter;
import co.breezing.module.R;
import co.breezing.module.eleven.reerq.RQDescription;
import co.breezing.module.five.adaptionReference.ReeReference;

public class FinalResults extends Activity {

	private Button okButton;
	private TextView ree_Str;
	private TextView rq_Str;
	private String high_str = "极大";
	private String low_str = "低";
	private String normal_str = "正常";

	private String rqOb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_final_results);
		Bundle extras = getIntent().getExtras();

		okButton = (Button) findViewById(R.id.OKButton);
		ree_Str = (TextView) findViewById(R.id.ree_str);
		rq_Str = (TextView) findViewById(R.id.rq_str);

		double rq = extras.getDouble("rq");
		double ree = extras.getDouble("ree");

		double ree_th = ReeReference.ree_th_calculator(Parameter.gender,
				Parameter.age, Parameter.height, Parameter.weight);

		int ree_diff = ReeReference.reeDescription(ree_th, ree);
		if (ree_diff == 1) {
			ree_Str.setText("ree = " + ree + "\t\t" + high_str);
			ree_Str.setTextColor(Color.GREEN);
		}
		else if (ree_diff == -1) {
			ree_Str.setText("ree = " + ree + "\t\t" + low_str);
			ree_Str.setTextColor(Color.RED);
		}
		else if (ree_diff == 2) {
			ree_Str.setText("ree = " + ree);
		}
		else {
			ree_Str.setText("ree = " + ree + "\t\t" + normal_str);
		}
		rqOb = RQDescription.rqDescription(rq);
		rq_Str.setText("rq = " + rq + "\t\t" + rqOb);

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent(getBaseContext(), FirstActivity.class);
				myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(myIntent);
				finish();
			}
		});
	}
}
