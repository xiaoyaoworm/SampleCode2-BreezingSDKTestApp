package co.breezing.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import co.breezing.module.R;

public class Results extends Activity {

	private TextView results;
	private TextView title;
	private Button okButton;

	private String type;
	private double rq;
	private double ree;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_results);
		Bundle extras = getIntent().getExtras();

		okButton = (Button) findViewById(R.id.OKButton);
		results = (TextView) findViewById(R.id.results_str);

		type = extras.getString("type");
		if (type.equals("training")) {
			String results_str = extras.getString("conclusion");
			results.setText(results_str);

		}
		else if (type.equals("normal")) {
			rq = extras.getDouble("rq");
			ree = extras.getDouble("ree");
			results.setText("您已通过正常测试！点击确定查看您的能量代谢值与呼吸熵。如要再次测试，请等待五分钟。");
		}

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (type.equals("training")) {
					Intent myIntent = new Intent(getBaseContext(), FirstActivity.class);
					myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(myIntent);
					finish();
				}
				else if (type.equals("normal")) {
					Intent myIntent = new Intent(getBaseContext(),
							FinalResults.class);
					myIntent.putExtra("type", "normal");
					myIntent.putExtra("rq", rq);
					myIntent.putExtra("ree", ree);
					startActivityForResult(myIntent, 0);
					finish();
				}
			}
		});
	}


}
