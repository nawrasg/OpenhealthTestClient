package es.libresoft.openhealth.android.test.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ManagerTestClient extends Activity {

	static String serviceName = new String("es.libresoft.openhealth.android.OPENHEALTH_SERVICE");

	public boolean initManager() {
		try {
			Intent intentDroid = new Intent(serviceName);
			startService(intentDroid);
		}catch (Exception e) {
			e.printStackTrace();
		}

		System.err.println("Service launched");
		return true;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		initManager();
	}
}